/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server.currentState;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.progress.StatisticsDtoModel;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskCurrentStateDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoExecutionStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IterativeTaskInformationType;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

/**
 * @author mederly
 */
public class IterativeInformationPanel extends BasePanel<TaskCurrentStateDto> {

    private static final Trace LOGGER = TraceManager.getTrace(IterativeInformationPanel.class);

    private static final String ID_EXECUTION_TIME = "executionTime";
    private static final String ID_OBJECTS_PROCESSED_SUCCESS = "objectsProcessedSuccess";
    private static final String ID_OBJECTS_PROCESSED_SUCCESS_TIME = "objectsProcessedSuccessTime";
    private static final String ID_LAST_OBJECT_PROCESSED_SUCCESS = "lastObjectProcessedSuccess";
    private static final String ID_LAST_OBJECT_PROCESSED_SUCCESS_TIME = "lastObjectProcessedSuccessTime";
    private static final String ID_OBJECTS_PROCESSED_FAILURE = "objectsProcessedFailure";
    private static final String ID_OBJECTS_PROCESSED_FAILURE_TIME = "objectsProcessedFailureTime";
    private static final String ID_LAST_OBJECT_PROCESSED_FAILURE = "lastObjectProcessedFailure";
    private static final String ID_LAST_OBJECT_PROCESSED_FAILURE_TIME = "lastObjectProcessedFailureTime";
    private static final String ID_LAST_ERROR = "lastError";
    private static final String ID_CURRENT_OBJECT_PROCESSED = "currentObjectProcessed";
    private static final String ID_CURRENT_OBJECT_PROCESSED_TIME = "currentObjectProcessedTime";
    private static final String ID_OBJECTS_TOTAL = "objectsTotal";

    // ugly hack - TODO replace with something serious
    // we cannot work with handler uri, because it exists only as long as the task executes
    private static final Collection<String> WALL_CLOCK_AVG_CATEGORIES = Arrays.asList(
            TaskCategory.BULK_ACTIONS, TaskCategory.IMPORTING_ACCOUNTS, TaskCategory.RECOMPUTATION, TaskCategory.RECONCILIATION,
            TaskCategory.UTIL       // this is a megahack: only utility tasks that count objects are DeleteTask and ShadowIntegrityCheck
    );

    private StatisticsDtoModel statisticsDtoModel;

    public IterativeInformationPanel(String id, IModel<TaskCurrentStateDto> model, final PageBase pageBase) {
        super(id, model);
        initLayout(pageBase);
    }

    protected void initLayout(PageBase pageBase) {

        Label executionTime = new Label(ID_EXECUTION_TIME, new IModel<String>() {
            @Override
            public String getObject() {
                TaskDto dto = getModel().getObject().getTaskDto();
                if (dto == null) {
                    return null;
                }
                Long started = dto.getLastRunStartTimestampLong();
                Long finished = dto.getLastRunFinishTimestampLong();
                if (started == null) {
                    return null;
                }
                if (TaskDtoExecutionStatus.RUNNING.equals(dto.getExecution()) || finished == null || finished < started) {
                    return getString("TaskStatePanel.message.executionTime.notFinished", formatDate(new Date(started), pageBase),
                            DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - started));
                } else {
                    return getString("TaskStatePanel.message.executionTime.finished",
                            formatDate(new Date(started), pageBase), formatDate(new Date(finished), pageBase),
                            DurationFormatUtils.formatDurationHMS(finished - started));
                }
            }
        });
        add(executionTime);

        Label processedSuccess = new Label(ID_OBJECTS_PROCESSED_SUCCESS, new IModel<String>() {
            @Override
            public String getObject() {
                TaskCurrentStateDto dto = getModelObject();
                if (dto == null) {
                    return null;
                }
                IterativeTaskInformationType info = dto.getIterativeTaskInformationType();
                if (info == null) {
                    return null;
                }
                if (info.getTotalSuccessCount() == 0) {
                    return "0";
                } else {
                    return getString("TaskStatePanel.message.objectsProcessed", info.getTotalSuccessCount());
                }
            }
        });
        add(processedSuccess);

        Label processedSuccessTime = new Label(ID_OBJECTS_PROCESSED_SUCCESS_TIME, new IModel<String>() {
            @Override
            public String getObject() {
                TaskCurrentStateDto dto = getModelObject();
                if (dto == null) {
                    return null;
                }
                IterativeTaskInformationType info = dto.getIterativeTaskInformationType();
                if (info == null) {
                    return null;
                }
                if (info.getTotalSuccessCount() == 0) {
                    return null;
                } else {
                    return getString("TaskStatePanel.message.objectsProcessedTime",
                            info.getTotalSuccessDuration()/1000,
                            info.getTotalSuccessDuration()/info.getTotalSuccessCount());
                }
            }
        });
        add(processedSuccessTime);

        Label lastProcessedSuccess = new Label(ID_LAST_OBJECT_PROCESSED_SUCCESS, new IModel<String>() {
            @Override
            public String getObject() {
                TaskCurrentStateDto dto = getModelObject();
                if (dto == null) {
                    return null;
                }
                IterativeTaskInformationType info = dto.getIterativeTaskInformationType();
                if (info == null) {
                    return null;
                }
                if (info.getLastSuccessObjectDisplayName() == null) {
                    return null;
                } else {
                    return getString("TaskStatePanel.message.lastObjectProcessed",
                            info.getLastSuccessObjectDisplayName());
                }
            }
        });
        add(lastProcessedSuccess);

        Label lastProcessedSuccessTime = new Label(ID_LAST_OBJECT_PROCESSED_SUCCESS_TIME, new IModel<String>() {
            @Override
            public String getObject() {
                TaskCurrentStateDto dto = getModelObject();
                if (dto == null) {
                    return null;
                }
                IterativeTaskInformationType info = dto.getIterativeTaskInformationType();
                if (info == null) {
                    return null;
                }
                if (info.getLastSuccessEndTimestamp() == null) {
                    return null;
                } else {
                    if (showAgo(dto)) {
                        return getString("TaskStatePanel.message.timeInfoWithDurationAndAgo",
                                formatDate(info.getLastSuccessEndTimestamp(), pageBase),
                                WebComponentUtil.formatDurationWordsForLocal(System.currentTimeMillis() -
                                        XmlTypeConverter.toMillis(info.getLastSuccessEndTimestamp()), true, true, pageBase),
                                info.getLastSuccessDuration());
                    } else {
                        return getString("TaskStatePanel.message.timeInfoWithDuration",
                                formatDate(info.getLastSuccessEndTimestamp(), pageBase),
                                info.getLastSuccessDuration());
                    }
                }
            }
        });
        add(lastProcessedSuccessTime);

        Label processedFailure = new Label(ID_OBJECTS_PROCESSED_FAILURE, new IModel<String>() {
            @Override
            public String getObject() {
                TaskCurrentStateDto dto = getModelObject();
                if (dto == null) {
                    return null;
                }
                IterativeTaskInformationType info = dto.getIterativeTaskInformationType();
                if (info == null) {
                    return null;
                }
                if (info.getTotalFailureCount() == 0) {
                    return "0";
                } else {
                    return getString("TaskStatePanel.message.objectsProcessed",
                            info.getTotalFailureCount());
                }
            }
        });
        add(processedFailure);

        Label processedFailureTime = new Label(ID_OBJECTS_PROCESSED_FAILURE_TIME, new IModel<String>() {
            @Override
            public String getObject() {
                TaskCurrentStateDto dto = getModelObject();
                if (dto == null) {
                    return null;
                }
                IterativeTaskInformationType info = dto.getIterativeTaskInformationType();
                if (info == null) {
                    return null;
                }
                if (info.getTotalFailureCount() == 0) {
                    return null;
                } else {
                    return getString("TaskStatePanel.message.objectsProcessedTime",
                            info.getTotalFailureDuration()/1000,
                            info.getTotalFailureDuration()/info.getTotalFailureCount());
                }
            }
        });
        add(processedFailureTime);

        Label lastProcessedFailure = new Label(ID_LAST_OBJECT_PROCESSED_FAILURE, new IModel<String>() {
            @Override
            public String getObject() {
                TaskCurrentStateDto dto = getModelObject();
                if (dto == null) {
                    return null;
                }
                IterativeTaskInformationType info = dto.getIterativeTaskInformationType();
                if (info == null) {
                    return null;
                }
                if (info.getLastFailureObjectDisplayName() == null) {
                    return null;
                } else {
                    return getString("TaskStatePanel.message.lastObjectProcessed",
                            info.getLastFailureObjectDisplayName());
                }
            }
        });
        add(lastProcessedFailure);

        Label lastProcessedFailureTime = new Label(ID_LAST_OBJECT_PROCESSED_FAILURE_TIME, new IModel<String>() {
            @Override
            public String getObject() {
                TaskCurrentStateDto dto = getModelObject();
                if (dto == null) {
                    return null;
                }
                IterativeTaskInformationType info = dto.getIterativeTaskInformationType();
                if (info == null) {
                    return null;
                }
                if (info.getLastFailureEndTimestamp() == null) {
                    return null;
                } else {
                    if (showAgo(dto)) {
                        return getString("TaskStatePanel.message.timeInfoWithDurationAndAgo",
                                formatDate(info.getLastFailureEndTimestamp(), pageBase),
                                WebComponentUtil.formatDurationWordsForLocal(System.currentTimeMillis() -
                                        XmlTypeConverter.toMillis(info.getLastFailureEndTimestamp()), true, true, pageBase),
                                info.getLastFailureDuration());
                    } else {
                        return getString("TaskStatePanel.message.timeInfoWithDuration",
                                formatDate(info.getLastFailureEndTimestamp(), pageBase),
                                info.getLastFailureDuration());
                    }
                }
            }
        });
        add(lastProcessedFailureTime);

        Label lastError = new Label(ID_LAST_ERROR, new IModel<String>() {
            @Override
            public String getObject() {
                TaskCurrentStateDto dto = getModelObject();
                if (dto == null) {
                    return null;
                }
                IterativeTaskInformationType info = dto.getIterativeTaskInformationType();
                if (info == null) {
                    return null;
                }
                return info.getLastFailureExceptionMessage();
            }
        });
        add(lastError);

        Label currentObjectProcessed = new Label(ID_CURRENT_OBJECT_PROCESSED, new IModel<String>() {
            @Override
            public String getObject() {
                TaskCurrentStateDto dto = getModelObject();
                if (dto == null) {
                    return null;
                }
                IterativeTaskInformationType info = dto.getIterativeTaskInformationType();
                if (info == null) {
                    return null;
                }
                return info.getCurrentObjectDisplayName();
            }
        });
        add(currentObjectProcessed);

        Label currentObjectProcessedTime = new Label(ID_CURRENT_OBJECT_PROCESSED_TIME, new IModel<String>() {
            @Override
            public String getObject() {
                TaskCurrentStateDto dto = getModelObject();
                if (dto == null) {
                    return null;
                }
                IterativeTaskInformationType info = dto.getIterativeTaskInformationType();
                if (info == null) {
                    return null;
                }
                if (info.getCurrentObjectStartTimestamp() == null) {
                    return null;
                } else {
                    return getString("TaskStatePanel.message.timeInfoWithAgo",
                            formatDate(info.getCurrentObjectStartTimestamp(), pageBase),
                            WebComponentUtil.formatDurationWordsForLocal(System.currentTimeMillis() -
                                    XmlTypeConverter.toMillis(info.getCurrentObjectStartTimestamp()), true, true, pageBase));
                }
            }
        });
        add(currentObjectProcessedTime);

        Label objectsTotal = new Label(ID_OBJECTS_TOTAL, (IModel<String>) () -> {
            TaskCurrentStateDto dto = getModelObject();
            if (dto == null) {
                return null;
            }
            IterativeTaskInformationType info = dto.getIterativeTaskInformationType();
            if (info == null) {
                return null;
            }
            int objectsTotal1 = info.getTotalSuccessCount() + info.getTotalFailureCount();
            if (WALL_CLOCK_AVG_CATEGORIES.contains(dto.getTaskDto().getCategory())) {
                Long avg = getWallClockAverage(dto, objectsTotal1);
                if (avg != null) {
                    long throughput = avg != 0 ? 60000 / avg : 0;       // TODO what if avg == 0?
                    return getString("TaskStatePanel.message.objectsTotal",
                            objectsTotal1, avg, throughput);
                }
            }
            return String.valueOf(objectsTotal1);
        });
        add(objectsTotal);
    }

    private String formatDate(XMLGregorianCalendar date, PageBase pageBase) {
        return formatDate(XmlTypeConverter.toDate(date), pageBase);
    }

    private String formatDate(Date date, PageBase pageBase) {
        if (date == null) {
            return null;
        }
        return WebComponentUtil.getLongDateTimeFormattedValue(date, pageBase);
    }

    protected boolean showAgo(TaskCurrentStateDto dto) {
        boolean showAgo = false;
        TaskDto taskDto = dto.getTaskDto();
        if (taskDto != null) {
            Long started = taskDto.getLastRunStartTimestampLong();
            Long finished = taskDto.getLastRunFinishTimestampLong();
            if (started != null && (finished == null || finished < started)) {
                showAgo = true;     // for all running tasks
            }
        }
        return showAgo;
    }

    private Long getWallClockAverage(TaskCurrentStateDto dto, int objectsTotal) {
        if (objectsTotal == 0) {
            return null;
        }
        if (dto == null || dto.getTaskDto() == null) {
            return null;
        }
        Long started = dto.getTaskDto().getLastRunStartTimestampLong();
        if (started == null) {
            return null;
        }
        Long finished = dto.getTaskDto().getLastRunFinishTimestampLong();
        if (finished == null || finished < started) {
            finished = System.currentTimeMillis();
        }
        return (finished - started) / objectsTotal;
    }

}
