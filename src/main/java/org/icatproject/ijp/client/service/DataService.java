package org.icatproject.ijp.client.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.icatproject.ijp.shared.Authenticator;
import org.icatproject.ijp.shared.DatafileOverview;
import org.icatproject.ijp.shared.DatasetOverview;
import org.icatproject.ijp.shared.ForbiddenException;
import org.icatproject.ijp.shared.GenericSearchSelections;
import org.icatproject.ijp.shared.InternalException;
import org.icatproject.ijp.shared.JobDTO;
import org.icatproject.ijp.shared.LoginResult;
import org.icatproject.ijp.shared.ParameterException;
import org.icatproject.ijp.shared.PortalUtils.OutputType;
import org.icatproject.ijp.shared.PortalUtils.ParameterValueType;
import org.icatproject.ijp.shared.SessionException;
import org.icatproject.ijp.shared.xmlmodel.JobType;
import org.icatproject.ijp.shared.xmlmodel.JobTypeMappings;
import org.icatproject.ijp.shared.xmlmodel.SearchItems;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("DataService")
public interface DataService extends RemoteService {
	List<DatasetOverview> getDatasetList(String sessionId, String datasetType,
			Map<String, List<String>> selectedSearchParamsMap, List<GenericSearchSelections> genericSearchSelectionsList)
			throws SessionException, InternalException;

	List<DatafileOverview> getDatafileList(String sessionId, String datasetType, Long datasetId,
			Map<String, List<String>> selectedSearchParamsMap, List<GenericSearchSelections> genericSearchSelectionsList)
			throws SessionException, InternalException;

	LinkedHashMap<String, String> getDatasetParameters(String sessionId, Long datasetId) throws SessionException,
			InternalException;

	LoginResult login(String plugin, Map<String, String> credentials) throws SessionException, InternalException;

	List<JobDTO> getJobsForUser(String sessionId) throws SessionException, InternalException, ForbiddenException,
			ParameterException;

	String getJobOutput(String sessionId, long jobId, OutputType outputType) throws SessionException,
			ForbiddenException, InternalException, ParameterException;

	SearchItems getDatasetSearchItems() throws InternalException;

	SearchItems getDatafileSearchItems() throws InternalException;

	List<String> getDatasetTypesList(String sessionId) throws SessionException, InternalException;

	JobTypeMappings getJobTypeMappings() throws InternalException;

	LinkedHashMap<String, ParameterValueType> getDatasetParameterTypesMap(String sessionId) throws SessionException,
			InternalException;

	LinkedHashMap<String, ParameterValueType> getDatafileParameterTypesMap(String sessionId) throws SessionException,
			InternalException;

	Map<Long, Map<String, Object>> getJobDatasetParametersForDatasets(String sessionId, String datasetType,
			List<Long> datasetIds) throws SessionException, InternalException;

	String submitBatch(String sessionId, JobType jobType, List<String> parameters) throws ParameterException,
			SessionException, InternalException, ForbiddenException;

	String submitInteractive(String sessionId, JobType jobType, List<String> parameters) throws InternalException,
			ForbiddenException, ParameterException, SessionException;

	// dummy methods to get other objects we want to use added to the GWT
	// SerializationPolicy
	Double addDoubleToSerializationPolicy(Double aDouble);

	String getDataUrl(String sessionId, List<Long> investigationIds, List<Long> datasetIds, List<Long> datafileIds,
			String outname);

	String getIdsUrlString();

	List<Authenticator> getAuthenticators();

	void cancelJob(String sessionId, long jobId) throws SessionException, ForbiddenException, InternalException,
			ParameterException;

	void deleteJob(String sessionId, long jobId) throws SessionException, ForbiddenException, InternalException,
			ParameterException;

}