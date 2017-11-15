/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.grpc.model.converter;

import com.sitewhere.grpc.kafka.model.KafkaModel.GEnrichedEventPayload;
import com.sitewhere.grpc.kafka.model.KafkaModel.GInboundEventPayload;
import com.sitewhere.grpc.kafka.model.KafkaModel.GPersistedEventPayload;
import com.sitewhere.grpc.model.CommonModel.GOptionalString;
import com.sitewhere.rest.model.microservice.kafka.payload.EnrichedEventPayload;
import com.sitewhere.rest.model.microservice.kafka.payload.InboundEventPayload;
import com.sitewhere.rest.model.microservice.kafka.payload.PersistedEventPayload;
import com.sitewhere.spi.SiteWhereException;
import com.sitewhere.spi.microservice.kafka.payload.IEnrichedEventPayload;
import com.sitewhere.spi.microservice.kafka.payload.IInboundEventPayload;
import com.sitewhere.spi.microservice.kafka.payload.IPersistedEventPayload;

/**
 * Convert model objects passed on Kafka topics.
 * 
 * @author Derek
 */
public class KafkaModelConverter {

    /**
     * Convert inbound event payload from GRPC to API.
     * 
     * @param grpc
     * @return
     * @throws SiteWhereException
     */
    public static InboundEventPayload asApiInboundEventPayload(GInboundEventPayload grpc) throws SiteWhereException {
	InboundEventPayload api = new InboundEventPayload();
	api.setSourceId(grpc.getSourceId());
	api.setHardwareId(grpc.getHardwareId());
	api.setOriginator(grpc.hasOriginator() ? grpc.getOriginator().getValue() : null);
	api.setEventCreateRequest(EventModelConverter.asApiDeviceEventCreateRequest(grpc.getEvent()));
	return api;
    }

    /**
     * Convert inbound event payload from API to GRPC.
     * 
     * @param api
     * @return
     * @throws SiteWhereException
     */
    public static GInboundEventPayload asGrpcInboundEventPayload(IInboundEventPayload api) throws SiteWhereException {
	GInboundEventPayload.Builder grpc = GInboundEventPayload.newBuilder();
	grpc.setSourceId(api.getSourceId());
	grpc.setHardwareId(api.getHardwareId());
	if (api.getOriginator() != null) {
	    grpc.setOriginator(GOptionalString.newBuilder().setValue(api.getOriginator()));
	}
	grpc.setEvent(EventModelConverter.asGrpcDeviceEventCreateRequest(api.getEventCreateRequest()));
	return grpc.build();
    }

    /**
     * Convert persisted event payload from GRPC to API.
     * 
     * @param grpc
     * @return
     * @throws SiteWhereException
     */
    public static PersistedEventPayload asApiPersisedEventPayload(GPersistedEventPayload grpc)
	    throws SiteWhereException {
	PersistedEventPayload api = new PersistedEventPayload();
	api.setHardwareId(grpc.getHardwareId());
	api.setEvent(EventModelConverter.asApiGenericDeviceEvent(grpc.getEvent()));
	return api;
    }

    /**
     * Convert persisted event payload from API to GRPC.
     * 
     * @param api
     * @return
     * @throws SiteWhereException
     */
    public static GPersistedEventPayload asGrpcPersistedEventPayload(IPersistedEventPayload api)
	    throws SiteWhereException {
	GPersistedEventPayload.Builder grpc = GPersistedEventPayload.newBuilder();
	grpc.setHardwareId(api.getHardwareId());
	grpc.setEvent(EventModelConverter.asGrpcGenericDeviceEvent(api.getEvent()));
	return grpc.build();
    }

    /**
     * Convert enriched event payload from GRPC to API.
     * 
     * @param grpc
     * @return
     * @throws SiteWhereException
     */
    public static EnrichedEventPayload asApiEnrichedEventPayload(GEnrichedEventPayload grpc) throws SiteWhereException {
	EnrichedEventPayload api = new EnrichedEventPayload();
	api.setEventContext(EventModelConverter.asApiDeviceEventContext(grpc.getContext()));
	api.setEvent(EventModelConverter.asApiGenericDeviceEvent(grpc.getEvent()));
	return api;
    }

    /**
     * Convert enriched event payload from API to GRPC.
     * 
     * @param api
     * @return
     * @throws SiteWhereException
     */
    public static GEnrichedEventPayload asGrpcEnrichedEventPayload(IEnrichedEventPayload api)
	    throws SiteWhereException {
	GEnrichedEventPayload.Builder grpc = GEnrichedEventPayload.newBuilder();
	grpc.setContext(EventModelConverter.asGrpcDeviceEventContext(api.getEventContext()));
	grpc.setEvent(EventModelConverter.asGrpcGenericDeviceEvent(api.getEvent()));
	return grpc.build();
    }
}