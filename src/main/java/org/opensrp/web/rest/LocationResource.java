package org.opensrp.web.rest;

import static org.opensrp.common.AllConstants.OpenSRPEvent.Form.SERVER_VERSION;
import static org.opensrp.web.Constants.DEFAULT_GET_ALL_IDS_LIMIT;
import static org.opensrp.web.Constants.DEFAULT_LIMIT;
import static org.opensrp.web.Constants.LIMIT;
import static org.opensrp.web.config.SwaggerDocStringHelper.GET_LOCATION_TREE_BY_ID_ENDPOINT;
import static org.opensrp.web.config.SwaggerDocStringHelper.GET_LOCATION_TREE_BY_ID_ENDPOINT_NOTES;
import static org.opensrp.web.config.SwaggerDocStringHelper.LOCATION_RESOURCE;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.opensrp.common.AllConstants.BaseEntity;
import org.opensrp.domain.CustomPhysicalLocation;
import org.opensrp.domain.LocationProperty;
import org.opensrp.domain.PhysicalLocation;
import org.opensrp.domain.StructureDetails;
import org.opensrp.search.LocationSearchBean;
import org.opensrp.search.LocationSearchBean.OrderByType;
import org.opensrp.service.PhysicalLocationService;
import org.opensrp.util.PropertiesConverter;
import org.opensrp.web.bean.Identifier;
import org.opensrp.web.bean.LocationSyncBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

@Controller
@RequestMapping(value = "/rest/location")
@Api(value = LOCATION_RESOURCE, produces = LOCATION_RESOURCE)
public class LocationResource {
	
	private static Logger logger = LoggerFactory.getLogger(LocationResource.class.toString());
	
	public static Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HHmm")
	        .registerTypeAdapter(LocationProperty.class, new PropertiesConverter()).create();
	
	public static final String IS_JURISDICTION = "is_jurisdiction";
	
	public static final String PARENT_ID = "parent_id";
	
	private static final String FALSE = "false";
	
	private static final String TRUE = "true";
	
	public static final String LOCATION_NAMES = "location_names";
	
	public static final String LATITUDE = "latitude";
	
	public static final String LONGITUDE = "longitude";
	
	public static final String RADIUS = "radius";
	
	public static final String RETURN_GEOMETRY = "return_geometry";
	
	public static final String PROPERTIES_FILTER = "properties_filter";
	
	public static final String JURISDICTION_IDS = "jurisdiction_ids";
	
	public static final String JURISDICTION_ID = "jurisdiction_id";
	
	public static final String PAGE_SIZE = "page_size";
	
	public static final String DEFAULT_PAGE_SIZE = "1000";
	
	public static final String NAME = "name";
	
	private PhysicalLocationService locationService;
	
	protected ObjectMapper objectMapper;
	
	@Autowired
	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}
	
	@Autowired
	public void setLocationService(PhysicalLocationService locationService) {
		this.locationService = locationService;
	}
	
	@RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = { MediaType.APPLICATION_JSON_VALUE })
	@ApiOperation(value = GET_LOCATION_TREE_BY_ID_ENDPOINT, notes = GET_LOCATION_TREE_BY_ID_ENDPOINT_NOTES)
	public ResponseEntity<String> getByUniqueId(@PathVariable("id") String id,
	                                            @RequestParam(value = IS_JURISDICTION, defaultValue = FALSE, required = false) boolean isJurisdiction,
	                                            @RequestParam(value = RETURN_GEOMETRY, defaultValue = TRUE, required = false) boolean returnGeometry) {
		
		return new ResponseEntity<>(gson.toJson(isJurisdiction ? locationService.getLocation(id, returnGeometry)
		        : locationService.getStructure(id, returnGeometry)), RestUtils.getJSONUTF8Headers(), HttpStatus.OK);
	}
	
	@RequestMapping(value = "/sync", method = RequestMethod.POST, consumes = { MediaType.APPLICATION_JSON_VALUE }, produces = { MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<String> getLocations(@RequestBody LocationSyncRequestWrapper locationSyncRequestWrapper) {
		long currentServerVersion = 0;
		try {
			currentServerVersion = locationSyncRequestWrapper.getServerVersion();
		}
		catch (NumberFormatException e) {
			logger.error("server version not a number");
		}
		
		Boolean isJurisdiction = locationSyncRequestWrapper.getIsJurisdiction();
		String locationNames = StringUtils.join(locationSyncRequestWrapper.getLocationNames(), ",");
		String parentIds = StringUtils.join(locationSyncRequestWrapper.getParentId(), ",");
		
		if (isJurisdiction) {
			if (StringUtils.isBlank(locationNames)) {
				return new ResponseEntity<>(gson.toJson(locationService.findLocationsByServerVersion(currentServerVersion)),
				        RestUtils.getJSONUTF8Headers(), HttpStatus.OK);
			}
			return new ResponseEntity<>(gson.toJson(locationService
			        .findLocationsByNames(locationNames, currentServerVersion)), RestUtils.getJSONUTF8Headers(),
			        HttpStatus.OK);
			
		} else {
			if (StringUtils.isBlank(parentIds)) {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
			return new ResponseEntity<>(gson.toJson(locationService.findStructuresByParentAndServerVersion(parentIds,
			    currentServerVersion)), HttpStatus.OK);
		}
	}
	
	// here for backward compatibility
	@RequestMapping(value = "/sync", method = RequestMethod.GET, produces = { MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<String> getLocationsTwo(@RequestParam(BaseEntity.SERVER_VERSIOIN) String serverVersion,
	                                              @RequestParam(value = IS_JURISDICTION, defaultValue = FALSE, required = false) boolean isJurisdiction,
	                                              @RequestParam(value = LOCATION_NAMES, required = false) String locationNames,
	                                              @RequestParam(value = PARENT_ID, required = false) String parentIds) {
		long currentServerVersion = 0;
		try {
			currentServerVersion = Long.parseLong(serverVersion);
		}
		catch (NumberFormatException e) {
			logger.error("server version not a number");
		}
		
		if (isJurisdiction) {
			if (StringUtils.isBlank(locationNames)) {
				return new ResponseEntity<>(gson.toJson(locationService.findLocationsByServerVersion(currentServerVersion)),
				        RestUtils.getJSONUTF8Headers(), HttpStatus.OK);
			}
			return new ResponseEntity<>(gson.toJson(locationService
			        .findLocationsByNames(locationNames, currentServerVersion)), RestUtils.getJSONUTF8Headers(),
			        HttpStatus.OK);
			
		} else {
			if (StringUtils.isBlank(parentIds)) {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
			return new ResponseEntity<>(gson.toJson(locationService.findStructuresByParentAndServerVersion(parentIds,
			    currentServerVersion)), HttpStatus.OK);
		}
	}
	
	@RequestMapping(method = RequestMethod.POST, consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE })
	public ResponseEntity<HttpStatus> create(@RequestBody String entity,
	                                         @RequestParam(value = IS_JURISDICTION, defaultValue = FALSE, required = false) boolean isJurisdiction) {
		try {
			PhysicalLocation location = gson.fromJson(entity, PhysicalLocation.class);
			location.setJurisdiction(isJurisdiction);
			locationService.add(location);
			return new ResponseEntity<>(HttpStatus.CREATED);
		}
		catch (JsonSyntaxException e) {
			logger.error("The request doesnt contain a valid location representation" + entity);
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}
	
	@RequestMapping(method = RequestMethod.PUT, consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE })
	public ResponseEntity<HttpStatus> update(@RequestBody String entity,
	                                         @RequestParam(value = IS_JURISDICTION, defaultValue = FALSE, required = false) boolean isJurisdiction) {
		try {
			PhysicalLocation location = gson.fromJson(entity, PhysicalLocation.class);
			location.setJurisdiction(isJurisdiction);
			locationService.update(location);
			return new ResponseEntity<>(HttpStatus.CREATED);
		}
		catch (JsonSyntaxException e) {
			logger.error("The request doesnt contain a valid location representation" + entity);
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}
	
	@RequestMapping(value = "/add", method = RequestMethod.POST, consumes = { MediaType.APPLICATION_JSON_VALUE,
	        MediaType.TEXT_PLAIN_VALUE })
	public ResponseEntity<String> saveBatch(@RequestBody String entity,
	                                        @RequestParam(value = IS_JURISDICTION, defaultValue = FALSE, required = false) boolean isJurisdiction) {
		try {
			Type listType = new TypeToken<List<PhysicalLocation>>() {}.getType();
			List<PhysicalLocation> locations = gson.fromJson(entity, listType);
			Set<String> locationWithErrors = locationService.saveLocations(locations, isJurisdiction);
			if (locationWithErrors.isEmpty())
				return new ResponseEntity<>("All Locations  processed", HttpStatus.CREATED);
			else
				return new ResponseEntity<>("Locations with Ids not processed: " + String.join(",", locationWithErrors),
				        HttpStatus.CREATED);
			
		}
		catch (JsonSyntaxException e) {
			logger.error("The request doesnt contain a valid location representation" + entity);
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}
	
	@RequestMapping(value = "/findWithCordinates", method = RequestMethod.GET, produces = { MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<String> getStructuresWithinCordinates(@RequestParam(value = LATITUDE) double latitude,
	                                                            @RequestParam(value = LONGITUDE) double longitude,
	                                                            @RequestParam(value = RADIUS) double radius) {
		
		Collection<StructureDetails> structures = locationService.findStructuresWithinRadius(latitude, longitude, radius);
		return new ResponseEntity<>(gson.toJson(structures), RestUtils.getJSONUTF8Headers(), HttpStatus.OK);
		
	}
	
	/**
	 * This methods provides an API endpoint that searches for jurisdictions and structures with the
	 * properties including parentId. It returns the Geometry optionally if @param returnGeometry is
	 * set to true.
	 * 
	 * @param isJurisdiction boolean which when true the search is done on jurisdictions and when
	 *            false search is on structures
	 * @param returnGeometry boolean which controls if geometry is returned
	 * @param propertiesFilters list of params with each param having name and value e.g name:House1
	 * @return the structures or jurisdictions matching the params
	 */
	@RequestMapping(value = "/findByProperties", method = RequestMethod.GET, produces = { MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<String> findByLocationProperties(@RequestParam(value = IS_JURISDICTION, defaultValue = FALSE, required = false) boolean isJurisdiction,
	                                                       @RequestParam(value = RETURN_GEOMETRY, defaultValue = FALSE, required = false) boolean returnGeometry,
	                                                       @RequestParam(value = PROPERTIES_FILTER, required = false) List<String> propertiesFilters) {
		
		String parentId = null;
		Map<String, String> filters = null;
		if (propertiesFilters != null) {
			filters = new HashMap<>();
			for (String filter : propertiesFilters) {
				String[] filterArray = filter.split(":");
				if (filterArray.length == 2
				        && (PARENT_ID.equalsIgnoreCase(filterArray[0]) || "parentId".equalsIgnoreCase(filterArray[0]))) {
					parentId = filterArray[1];
					
				} else if (filterArray.length == 2) {
					filters.put(filterArray[0], filterArray[1]);
				}
			}
		}
		if (isJurisdiction) {
			return new ResponseEntity<>(gson.toJson(locationService.findLocationsByProperties(returnGeometry, parentId,
			    filters)), RestUtils.getJSONUTF8Headers(), HttpStatus.OK);
		} else {
			return new ResponseEntity<>(gson.toJson(locationService.findStructuresByProperties(returnGeometry, parentId,
			    filters)), RestUtils.getJSONUTF8Headers(), HttpStatus.OK);
		}
		
	}
	
	/**
	 * This methods provides an API endpoint that searches for jurisdictions using a list of
	 * provided jurisdiction ids. It returns the Geometry optionally if @param returnGeometry is set
	 * to true.
	 * 
	 * @param returnGeometry boolean which controls if geometry is returned
	 * @param jurisdictionIds list of jurisdiction ids
	 * @return jurisdictions whose ids match the provided params
	 */
	@RequestMapping(value = "/findByJurisdictionIds", method = RequestMethod.GET, produces = { MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<String> findByJurisdictionIds(@RequestParam(value = RETURN_GEOMETRY, defaultValue = FALSE, required = false) boolean returnGeometry,
	                                                    @RequestParam(value = JURISDICTION_IDS, required = false) List<String> jurisdictionIds) {
		
		return new ResponseEntity<>(gson.toJson(locationService.findLocationsByIds(returnGeometry, jurisdictionIds)),
		        RestUtils.getJSONUTF8Headers(), HttpStatus.OK);
		
	}
	
	/**
	 * This methods provides an API endpoint that searches for a location and it's children using
	 * the provided location id It returns the Geometry optionally if @param returnGeometry is set
	 * to true.
	 * 
	 * @param returnGeometry boolean which controls if geometry is returned
	 * @param jurisdictionId location id
	 * @param pageSize number of records to be returned
	 * @return location together with it's children whose id matches the provided param
	 */
	@RequestMapping(value = "/findByIdWithChildren", method = RequestMethod.GET, produces = { MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<String> findByIdWithChildren(@RequestParam(value = RETURN_GEOMETRY, defaultValue = FALSE, required = false) boolean returnGeometry,
	                                                   @RequestParam(value = PAGE_SIZE, defaultValue = DEFAULT_PAGE_SIZE, required = false) int pageSize,
	                                                   @RequestParam(value = JURISDICTION_ID, required = false) String jurisdictionId) {
		
		return new ResponseEntity<>(gson.toJson(locationService.findLocationByIdWithChildren(returnGeometry, jurisdictionId,
		    pageSize)), RestUtils.getJSONUTF8Headers(), HttpStatus.OK);
		
	}
	
	/**
	 * This methods provides an API endpoint that searches for all structure ids sorted by server
	 * version ascending
	 *
	 * @param serverVersion serverVersion using to filter by
	 * @return A list of structure Ids and last server version
	 */
	@RequestMapping(value = "/findStructureIds", method = RequestMethod.GET, produces = { MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<Identifier> findIds(@RequestParam(value = SERVER_VERSION) long serverVersion) {
		
		Pair<List<String>, Long> structureIdsPair = locationService.findAllStructureIds(serverVersion,
		    DEFAULT_GET_ALL_IDS_LIMIT);
		Identifier identifiers = new Identifier();
		identifiers.setIdentifiers(structureIdsPair.getLeft());
		identifiers.setLastServerVersion(structureIdsPair.getRight());
		return new ResponseEntity<>(identifiers, RestUtils.getJSONUTF8Headers(), HttpStatus.OK);
		
	}
	
	/**
	 * Fetch structures or jurisdictions ordered by serverVersion ascending It returns the Geometry
	 * optionally if @param returnGeometry is set to true.
	 * 
	 * @param isJurisdiction boolean which when true the search is done on jurisdictions and when
	 *            false search is on structures
	 * @param returnGeometry boolean which controls if geometry is returned
	 * @param serverVersion serverVersion using to filter by
	 * @param limit upper limit on number os plas to fetch
	 * @return the structures or jurisdictions matching the params
	 */
	@RequestMapping(value = "/getAll", method = RequestMethod.GET, produces = { MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<String> getAll(@RequestParam(value = IS_JURISDICTION, defaultValue = FALSE, required = false) boolean isJurisdiction,
	                                     @RequestParam(value = RETURN_GEOMETRY, defaultValue = FALSE, required = false) boolean returnGeometry,
	                                     @RequestParam(value = BaseEntity.SERVER_VERSIOIN) long serverVersion,
	                                     @RequestParam(value = LIMIT, required = false) Integer limit) {
		
		Integer pageLimit = limit == null ? DEFAULT_LIMIT : limit;
		
		if (isJurisdiction) {
			return new ResponseEntity<>(gson.toJson(locationService.findAllLocations(returnGeometry, serverVersion,
			    pageLimit)), RestUtils.getJSONUTF8Headers(), HttpStatus.OK);
		} else {
			return new ResponseEntity<>(gson.toJson(locationService.findAllStructures(returnGeometry, serverVersion,
			    pageLimit)), RestUtils.getJSONUTF8Headers(), HttpStatus.OK);
		}
		
	}
	
	/**
	 * This methods provides an API endpoint that searches for all location ids sorted by server
	 * version ascending
	 *
	 * @return A list of location Ids and last server version
	 */
	@RequestMapping(value = "/findLocationIds", method = RequestMethod.GET, produces = { MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<Identifier> findLocationIds(@RequestParam(value = SERVER_VERSION) long serverVersion) {
		
		Pair<List<String>, Long> locationIdsPair = locationService.findAllLocationIds(serverVersion,
		    DEFAULT_GET_ALL_IDS_LIMIT);
		Identifier identifiers = new Identifier();
		identifiers.setIdentifiers(locationIdsPair.getLeft());
		identifiers.setLastServerVersion(locationIdsPair.getRight());
		
		return new ResponseEntity<>(identifiers, RestUtils.getJSONUTF8Headers(), HttpStatus.OK);
		
	}
	
	@RequestMapping(value = "/search-location", method = RequestMethod.GET, produces = { MediaType.APPLICATION_JSON_VALUE })
	@ResponseBody
	public ResponseEntity<String> searchLocationByTagOrName(@RequestParam(value = "locationTagId", required = false) Long locationTagId,
	                                                        @RequestParam(value = "name", required = false) String name,
	                                                        @RequestParam(value = "parentId", required = false) Long parentId,
	                                                        @RequestParam(value = "status", required = false) String status,
	                                                        @RequestParam(value = "orderByFieldName", required = false) String orderByFieldName,
	                                                        @RequestParam(value = "orderByType", required = false) String orderByType,
	                                                        @RequestParam(value = "pageSize", required = false) Integer pageSize,
	                                                        @RequestParam(value = "pageNumber", required = false) Integer pageNumber)
	    throws JsonProcessingException {
		LocationSyncBean locationSyncBean = new LocationSyncBean();
		LocationSearchBean locationSearchBean = new LocationSearchBean();
		
		try {
			locationSearchBean.setName(name);
			locationSearchBean.setLocationTagId(locationTagId);
			locationSearchBean.setOrderByFieldName(orderByFieldName);
			if (orderByType != null) {
				locationSearchBean.setOrderByType(OrderByType.valueOf(orderByType));
			}
			locationSearchBean.setPageNumber(pageNumber);
			locationSearchBean.setPageSize(pageSize);
			locationSearchBean.setStatus(status);
			locationSearchBean.setParentId(parentId);
			List<CustomPhysicalLocation> locations = locationService.searchLocations(locationSearchBean);
			
			locationSyncBean.setCustomLocations(locations);
			int total = 0;
			if (pageNumber != null && pageNumber == 1) {
				total = locationService.countSearchLocations(locationSearchBean);
			}
			locationSyncBean.setTotal(total);
		}
		catch (IllegalArgumentException e) {
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<>(objectMapper.writeValueAsString(locationSyncBean), RestUtils.getJSONUTF8Headers(),
		        HttpStatus.OK);
		
	}
	
	static class LocationSyncRequestWrapper {
		
		@JsonProperty("is_jurisdiction")
		private Boolean isJurisdiction;
		
		@JsonProperty("location_names")
		private List<String> locationNames;
		
		@JsonProperty("parent_id")
		private List<String> parentId;
		
		@JsonProperty
		private long serverVersion;
		
		public Boolean getIsJurisdiction() {
			return isJurisdiction;
		}
		
		public List<String> getLocationNames() {
			return locationNames;
		}
		
		public List<String> getParentId() {
			return parentId;
		}
		
		public long getServerVersion() {
			return serverVersion;
		}
	}
	
}
