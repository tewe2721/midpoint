/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.impl;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.model.impl.security.NodeAuthenticationToken;
import com.evolveum.midpoint.model.impl.security.SecurityHelper;
import com.evolveum.midpoint.model.impl.util.RestServiceUtil;
import com.evolveum.midpoint.repo.api.CacheDispatcher;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskConstants;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchedulerInformationType;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import java.io.File;
import java.io.FileInputStream;

/**
 * REST service used for inter-cluster communication.
 *
 * These methods are NOT to be called generally by clients.
 * They are to be called internally by midPoint running on other cluster nodes.
 *
 * So the usual form of authentication will be CLUSTER (a.k.a. node authentication).
 * However, for diagnostic purposes we might allow also administrator access sometimes in the future.
 */
@Service
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, RestServiceUtil.APPLICATION_YAML})
public class ClusterRestService {

	public static final String CLASS_DOT = ClusterRestService.class.getName() + ".";

	private static final String OPERATION_EXECUTE_CLUSTER_EVENT = CLASS_DOT + "executeClusterEvent";
	private static final String OPERATION_GET_LOCAL_SCHEDULER_INFORMATION = CLASS_DOT + "getLocalSchedulerInformation";
	private static final String OPERATION_STOP_LOCAL_SCHEDULER = CLASS_DOT + "stopLocalScheduler";
	private static final String OPERATION_START_LOCAL_SCHEDULER = CLASS_DOT + "startLocalScheduler";
	private static final String OPERATION_STOP_LOCAL_TASK = CLASS_DOT + "stopLocalTask";

	private static final String OPERATION_GET_REPORT_FILE = CLASS_DOT + "getReportFile";
	private static final String OPERATION_DELETE_REPORT_FILE = CLASS_DOT + "deleteReportFile";

	private static final String EXPORT_DIR = "export/";

	@Autowired private SecurityHelper securityHelper;
	@Autowired private TaskManager taskManager;
	@Autowired private MidpointConfiguration midpointConfiguration;

	@Autowired private CacheDispatcher cacheDispatcher;

	private static final Trace LOGGER = TraceManager.getTrace(ClusterRestService.class);

	public ClusterRestService() {
		// nothing to do
	}

	@POST
	@Path("/event/{type}")
	@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, RestServiceUtil.APPLICATION_YAML})
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, RestServiceUtil.APPLICATION_YAML})
	public Response executeClusterEvent(@PathParam("type") String type, @Context MessageContext mc) {
		Task task = RestServiceUtil.initRequest(mc);
		OperationResult result = new OperationResult(OPERATION_EXECUTE_CLUSTER_EVENT);

		Response response;
		try {
			checkNodeAuthentication();

			String oid = "";
			Class<? extends ObjectType> clazz = ObjectTypes.getClassFromRestType(type);
			cacheDispatcher.dispatch(clazz, oid);

			result.recordSuccess();
			response = RestServiceUtil.createResponse(Status.OK, result);
		} catch (Throwable t) {
			response = RestServiceUtil.handleException(result, t);
		}
		finishRequest(task);
		return response;
	}

	@GET
	@Path(TaskConstants.GET_LOCAL_SCHEDULER_INFORMATION_REST_PATH)
	@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, RestServiceUtil.APPLICATION_YAML})
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, RestServiceUtil.APPLICATION_YAML})
	public Response getLocalSchedulerInformation(@Context MessageContext mc) {
		Task task = RestServiceUtil.initRequest(mc);
		OperationResult result = new OperationResult(OPERATION_GET_LOCAL_SCHEDULER_INFORMATION);

		Response response;
		try {
			checkNodeAuthentication();
			SchedulerInformationType schedulerInformation = taskManager.getLocalSchedulerInformation(result);
			response = RestServiceUtil.createResponse(Status.OK, schedulerInformation, result);
		} catch (Throwable t) {
			response = RestServiceUtil.handleException(result, t);
		}
		result.computeStatus();
		finishRequest(task);
		return response;
	}

	@POST
	@Path(TaskConstants.STOP_LOCAL_SCHEDULER_REST_PATH)
	@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, RestServiceUtil.APPLICATION_YAML})
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, RestServiceUtil.APPLICATION_YAML})
	public Response stopLocalScheduler(@Context MessageContext mc) {
		Task task = RestServiceUtil.initRequest(mc);
		OperationResult result = new OperationResult(OPERATION_STOP_LOCAL_SCHEDULER);

		Response response;
		try {
			checkNodeAuthentication();
			taskManager.stopLocalScheduler(result);
			response = RestServiceUtil.createResponse(Status.OK, result);
		} catch (Throwable t) {
			response = RestServiceUtil.handleException(result, t);
		}
		result.computeStatus();
		finishRequest(task);
		return response;
	}

	@POST
	@Path(TaskConstants.START_LOCAL_SCHEDULER_REST_PATH)
	@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, RestServiceUtil.APPLICATION_YAML})
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, RestServiceUtil.APPLICATION_YAML})
	public Response startLocalScheduler(@Context MessageContext mc) {
		Task task = RestServiceUtil.initRequest(mc);
		OperationResult result = new OperationResult(OPERATION_START_LOCAL_SCHEDULER);

		Response response;
		try {
			checkNodeAuthentication();
			taskManager.startLocalScheduler(result);
			response = RestServiceUtil.createResponse(Status.OK, result);
		} catch (Throwable t) {
			response = RestServiceUtil.handleException(result, t);
		}
		result.computeStatus();
		finishRequest(task);
		return response;
	}

	@SuppressWarnings("RSReferenceInspection")
	@POST
	@Path(TaskConstants.STOP_LOCAL_TASK_REST_PATH_PREFIX + "{oid}" + TaskConstants.STOP_LOCAL_TASK_REST_PATH_SUFFIX)
	@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, RestServiceUtil.APPLICATION_YAML})
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, RestServiceUtil.APPLICATION_YAML})
	public Response stopLocalTask(@PathParam("oid") String oid, @Context MessageContext mc) {
		Task task = RestServiceUtil.initRequest(mc);
		OperationResult result = new OperationResult(OPERATION_STOP_LOCAL_TASK);

		Response response;
		try {
			checkNodeAuthentication();
			taskManager.stopLocalTask(oid, result);
			response = RestServiceUtil.createResponse(Status.OK, result);
		} catch (Throwable t) {
			response = RestServiceUtil.handleException(result, t);
		}
		result.computeStatus();
		finishRequest(task);
		return response;
	}

	public static final String REPORT_FILE_PATH = "/reportFiles";
	public static final String REPORT_FILE_FILENAME_PARAMETER = "filename";

	@GET
	@Path(REPORT_FILE_PATH)
	@Produces("application/octet-stream")
	public Response getReportFile(@QueryParam(REPORT_FILE_FILENAME_PARAMETER) String fileName, @Context MessageContext mc) {
		Task task = RestServiceUtil.initRequest(mc);
		OperationResult result = new OperationResult(OPERATION_GET_REPORT_FILE);

		Response response;
		try {
			checkNodeAuthentication();
			FileResolution resolution = resolveFile(fileName);
			if (resolution.status == null) {
				StreamingOutput streaming = outputStream -> {
					try (FileInputStream fileInputStream = new FileInputStream(resolution.file)) {
						IOUtils.copy(fileInputStream, outputStream);
					}
				};
				response = Response.ok(streaming).build();          // we are only interested in the content, not in its type nor length
			} else {
				response = Response.status(resolution.status).build();
			}
			result.computeStatus();
		} catch (Throwable t) {
			response = RestServiceUtil.handleException(null, t);        // we don't return the operation result
		}
		finishRequest(task);
		return response;
	}

	@DELETE
	@Path(REPORT_FILE_PATH)
	public Response deleteReportFile(@QueryParam(REPORT_FILE_FILENAME_PARAMETER) String fileName, @Context MessageContext mc) {
		Task task = RestServiceUtil.initRequest(mc);
		OperationResult result = new OperationResult(OPERATION_DELETE_REPORT_FILE);

		Response response;
		try {
			checkNodeAuthentication();
			FileResolution resolution = resolveFile(fileName);
			if (resolution.status == null) {
				if (!resolution.file.delete()) {
					LOGGER.warn("Couldn't delete report output file {}", resolution.file);
				}
				response = Response.ok().build();
			} else {
				response = Response.status(resolution.status).build();
			}
			result.computeStatus();
		} catch (Throwable t) {
			response = RestServiceUtil.handleException(null, t);          // we don't return the operation result
		}
		finishRequest(task);
		return response;
	}

	static class FileResolution {
		File file;
		Status status;
	}

	private FileResolution resolveFile(String fileName) {
		FileResolution rv = new FileResolution();
		rv.file = new File(midpointConfiguration.getMidpointHome() + EXPORT_DIR + fileName);

		if (forbiddenFileName(fileName)) {
			LOGGER.warn("File name '{}' is forbidden", fileName);
			rv.status = Status.FORBIDDEN;
		} else if (!rv.file.exists()) {
			LOGGER.warn("Report output file '{}' does not exist", rv.file);
			rv.status = Status.NOT_FOUND;
		} else if (rv.file.isDirectory()) {
			LOGGER.warn("Report output file '{}' is a directory", rv.file);
			rv.status = Status.FORBIDDEN;
		}
		return rv;
	}

	private void finishRequest(Task task) {
		RestServiceUtil.finishRequest(task, securityHelper);
	}

	private boolean forbiddenFileName(String fileName) {
		return fileName.contains("/../");
	}

	private void checkNodeAuthentication() throws SecurityViolationException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (!(authentication instanceof NodeAuthenticationToken)) {
			throw new SecurityViolationException("Node authentication is expected but not present");
		}
		// TODO consider allowing administrator access here as well
	}

}
