package it.bz.noi.trafficeventroadworkbz;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.uuid.Generators;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;
import it.bz.idm.bdp.dto.EventDto;
import it.bz.noi.trafficeventroadworkbz.configuration.TrafficEventRoadworkBZConfiguration;
import it.bz.noi.trafficeventroadworkbz.model.TrafficEventRoadworkBZModel;
import it.bz.noi.trafficeventroadworkbz.pusher.TrafficEventRoadworkBZJsonPusher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component
public class MainTrafficEventRoadworkBZ {

    private static final Logger LOG = LogManager.getLogger(MainTrafficEventRoadworkBZ.class);

    @Autowired
    private TrafficEventRoadworkBZConnector trafficEventRoadworkBZConnector;
    @Autowired
    private TrafficEventRoadworkBZConfiguration configuration;
    @Autowired
    private TrafficEventRoadworkBZJsonPusher pusher;

    public void execute() {
        LOG.info("MainTrafficEventRoadworks execute");
        try {
            List<TrafficEventRoadworkBZModel> trafficEventRoadworkList = trafficEventRoadworkBZConnector.getTrafficEventRoadworksModelList();
            LOG.debug("got {} traffic events", trafficEventRoadworkList.size());
            List<EventDto> eventDtoList = new ArrayList<>();

            for (TrafficEventRoadworkBZModel trafficEventRoadwork : trafficEventRoadworkList) {
                EventDto eventDto = new EventDto();

                eventDto.setId(generateUuid(trafficEventRoadwork));
                eventDto.setCategory(trafficEventRoadwork.getTycodeIt() + " | " + trafficEventRoadwork.getTycodeDe());
                eventDto.setOrigin(configuration.getOrigin());
                eventDto.setDescription(trafficEventRoadwork.getDescriptionIt() + " | " + trafficEventRoadwork.getDescriptionDe());

                if(trafficEventRoadwork.getX() != null && trafficEventRoadwork.getY() != null) {
                    GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
                    Coordinate coordinate = new Coordinate(trafficEventRoadwork.getX(), trafficEventRoadwork.getY());
                    Point point = geometryFactory.createPoint(coordinate);
                    eventDto.setWktGeometry(point.toText());
                }

                eventDto.setEventStart(trafficEventRoadwork.getBeginDate().toEpochDay());
                if(trafficEventRoadwork.getEndDate() != null)
                    eventDto.setEventEnd(trafficEventRoadwork.getEndDate().toEpochDay());

                eventDto.getMetaData().put("json_featuretype", trafficEventRoadwork.getJson_featuretype());
                eventDto.getMetaData().put("publisherDateTime", trafficEventRoadwork.getPublisherDateTime());
                eventDto.getMetaData().put("tycodeValue", trafficEventRoadwork.getTycodeValue());
                eventDto.getMetaData().put("tycodeDe", trafficEventRoadwork.getTycodeDe());
                eventDto.getMetaData().put("tycodeIt", trafficEventRoadwork.getTycodeIt());
                eventDto.getMetaData().put("subTycodeValue", trafficEventRoadwork.getSubTycodeValue());
                eventDto.getMetaData().put("subTycodeDe", trafficEventRoadwork.getSubTycodeDe());
                eventDto.getMetaData().put("subTycodeIt", trafficEventRoadwork.getSubTycodeIt());
                eventDto.getMetaData().put("placeDe", trafficEventRoadwork.getPlaceDe());
                eventDto.getMetaData().put("placeIt", trafficEventRoadwork.getPlaceIt());
                eventDto.getMetaData().put("actualMail", trafficEventRoadwork.getActualMail());
                eventDto.getMetaData().put("messageId", trafficEventRoadwork.getMessageId());
                eventDto.getMetaData().put("messageStatus", trafficEventRoadwork.getMessageStatus());
                eventDto.getMetaData().put("messageZoneId", trafficEventRoadwork.getMessageZoneId());
                eventDto.getMetaData().put("messageZoneDescDe", trafficEventRoadwork.getMessageZoneDescDe());
                eventDto.getMetaData().put("messageZoneDescIt", trafficEventRoadwork.getMessageZoneDescIt());
                eventDto.getMetaData().put("messageGradId", trafficEventRoadwork.getMessageGradId());
                eventDto.getMetaData().put("messageGradDescDe", trafficEventRoadwork.getMessageGradDescDe());
                eventDto.getMetaData().put("messageGradDescIt", trafficEventRoadwork.getMessageGradDescIt());
                eventDto.getMetaData().put("messageStreetId", trafficEventRoadwork.getMessageStreetId());
                eventDto.getMetaData().put("messageStreetWapDescDe", trafficEventRoadwork.getMessageStreetWapDescDe());
                eventDto.getMetaData().put("messageStreetWapDescIt", trafficEventRoadwork.getMessageStreetWapDescIt());
                eventDto.getMetaData().put("messageStreetInternetDescDe", trafficEventRoadwork.getMessageStreetInternetDescDe());
                eventDto.getMetaData().put("messageStreetInternetDescIt", trafficEventRoadwork.getMessageStreetInternetDescIt());
                eventDto.getMetaData().put("messageStreetNr", trafficEventRoadwork.getMessageStreetNr());
                eventDto.getMetaData().put("messageStreetHierarchie", trafficEventRoadwork.getMessageStreetHierarchie());
                eventDto.getMetaData().put("messageTypeId", trafficEventRoadwork.getMessageTypeId());
                eventDto.getMetaData().put("messageTypeDescDe", trafficEventRoadwork.getMessageTypeDescDe());
                eventDto.getMetaData().put("messageTypeDescIt", trafficEventRoadwork.getMessageTypeDescIt());

                eventDtoList.add(eventDto);
            }

            pusher.addEvents(eventDtoList);
        } catch (Exception e) {
            LOG.error("reading traffic event failed ", e);
        }
    }

    private String generateUuid(TrafficEventRoadworkBZModel trafficEventRoadwork) throws JsonProcessingException {
        HashMap<String, Object> uuidMap = new HashMap<>();
        uuidMap.put("beginDate", trafficEventRoadwork.getBeginDate());
        uuidMap.put("endDate", trafficEventRoadwork.getEndDate());
        uuidMap.put("messageId", trafficEventRoadwork.getMessageId());
        uuidMap.put("messageTypeId", trafficEventRoadwork.getMessageTypeId());
        uuidMap.put("X", trafficEventRoadwork.getX());
        uuidMap.put("Y", trafficEventRoadwork.getY());

        ObjectMapper mapper = new ObjectMapper();
        String uuidNameJson = mapper.writer().writeValueAsString(uuidMap);

        return Generators.nameBasedGenerator(configuration.getUuidNamescpace()).generate(uuidNameJson).toString();
    }
}