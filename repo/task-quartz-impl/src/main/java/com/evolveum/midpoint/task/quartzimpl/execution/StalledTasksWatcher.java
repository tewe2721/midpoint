/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.execution;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.quartzimpl.RunningTaskQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.TaskQuartzImplUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import java.util.*;

/**
 * Watches whether a task is stalled.
 *
 * @author Pavol Mederly
 */
public class StalledTasksWatcher {

    private static final transient Trace LOGGER = TraceManager.getTrace(StalledTasksWatcher.class);

    private static final String DOT_CLASS = StalledTasksWatcher.class.getName() + ".";

    private static final List<String> CATEGORIES_TO_SKIP = Collections.singletonList(TaskCategory.ASYNCHRONOUS_UPDATE);

    private TaskManagerQuartzImpl taskManager;

    private static class ProgressInformation {
        long measurementTimestamp;
        long measuredProgress;
        long lastStartedTimestamp;
        long lastNotificationIssuedTimestamp;
        private ProgressInformation(long measurementTimestamp, long measuredProgress, long lastStartedTimestamp) {
            this.measurementTimestamp = measurementTimestamp;
            this.measuredProgress = measuredProgress;
            this.lastStartedTimestamp = lastStartedTimestamp;
        }
        @Override
        public String toString() {
            return "ProgressInformation{" +
                    "measurementTimestamp=" + measurementTimestamp + "/" + (measurementTimestamp != 0 ? new Date(measurementTimestamp) : "") +
                    ", measuredProgress=" + measuredProgress +
                    ", lastStartedTimestamp=" + lastStartedTimestamp + "/" + (lastStartedTimestamp != 0 ? new Date(lastStartedTimestamp) : "") +
                    ", lastNotificationIssuedTimestamp=" + lastNotificationIssuedTimestamp + "/" + (lastNotificationIssuedTimestamp != 0 ? new Date(lastNotificationIssuedTimestamp) : "") +
                    '}';
        }
    }

    private Map<String,ProgressInformation> lastProgressMap = new HashMap<>();

    public StalledTasksWatcher(TaskManagerQuartzImpl taskManager) {
        this.taskManager = taskManager;
    }

    public void checkStalledTasks(OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(DOT_CLASS + "checkStalledTasks");

        Map<String, RunningTaskQuartzImpl> runningTasks = taskManager.getLocallyRunningTaskInstances();
        LOGGER.trace("checkStalledTasks: running tasks = {}", runningTasks);

        for (RunningTaskQuartzImpl task : runningTasks.values()) {
            if (CATEGORIES_TO_SKIP.contains(task.getCategory())) {
                continue;
            }
            long currentTimestamp = System.currentTimeMillis();
            long lastStartedTimestamp = task.getLastRunStartTimestamp() != null ? task.getLastRunStartTimestamp() : 0L;
            Long heartbeatProgressInfo = task.getHandler().heartbeat(task);
            long realProgress;
            if (heartbeatProgressInfo != null) {
                realProgress = heartbeatProgressInfo;
            } else {
                try {
                    realProgress = taskManager.getTask(task.getOid(), result).getProgress();
                } catch (ObjectNotFoundException e) {
                    LoggingUtils.logException(LOGGER, "Task {} cannot be checked for staleness because it is gone", e, task);
                    continue;
                } catch (SchemaException e) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Task {} cannot be checked for staleness because of schema exception", e, task);
                    continue;
                }
            }

            ProgressInformation lastProgressEntry = lastProgressMap.get(task.getTaskIdentifier());

            LOGGER.trace("checkStalledTasks: considering ({}, {}, {}), last information = {}", task, lastStartedTimestamp,
                    realProgress, lastProgressEntry);

            // check and/or update the last progress information
            if (hasEntryChanged(lastProgressEntry, lastStartedTimestamp, realProgress)) {
                lastProgressMap.put(task.getTaskIdentifier(), new ProgressInformation(currentTimestamp, realProgress, lastStartedTimestamp));
            } else {
                if (isEntryStalled(currentTimestamp, lastProgressEntry)) {
                    if (currentTimestamp - lastProgressEntry.lastNotificationIssuedTimestamp > taskManager.getConfiguration().getStalledTasksRepeatedNotificationInterval() * 1000L) {
                        LOGGER.error("Task {} seems to be stalled (started {}; progress is still {}, observed since {}){}",
                                task,
                                new Date(lastProgressEntry.lastStartedTimestamp),
                                lastProgressEntry.measuredProgress,
                                new Date(lastProgressEntry.measurementTimestamp),
                                lastProgressEntry.lastNotificationIssuedTimestamp != 0 ?
                                    " [this is a repeated notification]" : "");
                        lastProgressEntry.lastNotificationIssuedTimestamp = currentTimestamp;
                        try {
                            taskManager.recordTaskThreadsDump(task.getOid(), SchemaConstants.INTERNAL_URI, result);
                        } catch (SchemaException|ObjectNotFoundException|ObjectAlreadyExistsException|RuntimeException e) {
                            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't record thread dump for stalled task {}", e, task);
                        }
                    }
                }
            }
        }

        // clean-up obsolete progress entries
        lastProgressMap.keySet().removeIf(s -> !runningTasks.containsKey(s));

        LOGGER.trace("checkStalledTasks lastProgress map after cleaning up = {}", lastProgressMap);
    }

    public Long getStalledSinceForTask(TaskType taskType) {
        ProgressInformation lastProgressEntry = lastProgressMap.get(taskType.getTaskIdentifier());
        if (taskType.getExecutionStatus() != TaskExecutionStatusType.RUNNABLE ||
                hasEntryChanged(lastProgressEntry, TaskQuartzImplUtil.xmlGCtoMillis(taskType.getLastRunStartTimestamp()),
                    taskType.getProgress() != null ? taskType.getProgress() : 0L)) {
            return null;
        } else {
            if (isEntryStalled(System.currentTimeMillis(), lastProgressEntry)) {
                return lastProgressEntry.measurementTimestamp;
            } else {
                return null;
            }
        }
    }

    private boolean isEntryStalled(long currentTimestamp, ProgressInformation lastProgressEntry) {
        return currentTimestamp - lastProgressEntry.measurementTimestamp > taskManager.getConfiguration().getStalledTasksThreshold() * 1000L;
    }

    private boolean hasEntryChanged(ProgressInformation lastProgressEntry, long lastRunStartTimestamp, long realProgress) {
        return lastProgressEntry == null
                || lastRunStartTimestamp != lastProgressEntry.lastStartedTimestamp
                || realProgress != lastProgressEntry.measuredProgress;
    }
}

