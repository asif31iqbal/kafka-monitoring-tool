package com.symantec.cpe.analytics.resources.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.symantec.cpe.analytics.KafkaMonitorConfiguration;
import com.symantec.cpe.analytics.core.ResponseMessage;
import com.symantec.cpe.analytics.core.kafka.KafkaOffsetMonitor;
import com.symantec.cpe.analytics.core.managed.ZKClient;
import com.symantec.cpe.analytics.kafka.KafkaConsumerOffsetUtil;
import kafka.utils.ZKStringSerializer$;
import org.I0Itec.zkclient.ZkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Path("/kafka")
@Consumes(MediaType.APPLICATION_JSON)
public class KafkaResource {
    private KafkaMonitorConfiguration kafkaConfiguration;
    private static final Logger LOG = LoggerFactory.getLogger(KafkaResource.class);
    private ZKClient zkClient;
    ZkClient iotecZkClient;

    public KafkaResource(KafkaMonitorConfiguration kafkaConfiguration, ZKClient zkClient) {
        this.kafkaConfiguration = kafkaConfiguration;
        this.zkClient = zkClient;
        if (kafkaConfiguration.isPushToStatsD()) {
            KafkaConsumerOffsetUtil kafkaConsumerOffsetUtil = KafkaConsumerOffsetUtil.getInstance(kafkaConfiguration, zkClient);
            kafkaConsumerOffsetUtil.setupMonitoring();
        }
        iotecZkClient = new ZkClient(kafkaConfiguration.getZookeeperUrls(), 5000, 5000, ZKStringSerializer$.MODULE$);
    }

    @Path("/offset")
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_HTML})
    public Response getKafkaConsumerOffset(@DefaultValue("all") @QueryParam("consumerType") String consumerType,
                                           @DefaultValue("json") @QueryParam("outputType") String outputType) {
        String output = null;
        String responseType = MediaType.APPLICATION_JSON;
        try {
            List<KafkaOffsetMonitor> kafkaOffsetMonitors = new ArrayList<>();
            KafkaConsumerOffsetUtil kafkaConsumerOffsetUtil = KafkaConsumerOffsetUtil.getInstance(kafkaConfiguration, zkClient);
            if (consumerType.equalsIgnoreCase("spout")) {
                kafkaOffsetMonitors.addAll(kafkaConsumerOffsetUtil.getSpoutKafkaOffsetMonitors());
            } else if (consumerType.equalsIgnoreCase("regular")) {
                kafkaOffsetMonitors.addAll(kafkaConsumerOffsetUtil.getRegularKafkaOffsetMonitors());
            } else if (consumerType.equalsIgnoreCase("all")) {
                kafkaOffsetMonitors.addAll(kafkaConsumerOffsetUtil.getSpoutKafkaOffsetMonitors());
                kafkaOffsetMonitors.addAll(kafkaConsumerOffsetUtil.getRegularKafkaOffsetMonitors());
            } else {
                LOG.info("Invalid Consumer Type: " + consumerType);
                return Response.status(Response.Status.BAD_REQUEST).entity(new ResponseMessage("Invalid Consumer Type. Valid types are 'spout', 'regular', 'all'")).type(responseType).build();
            }
            if (outputType.equals("html")) {
                responseType = MediaType.TEXT_HTML;
                output = kafkaConsumerOffsetUtil.htmlOutput(kafkaOffsetMonitors);
            } else {
                ObjectMapper mapper = new ObjectMapper();
                output = mapper.writeValueAsString(kafkaOffsetMonitors);
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ResponseMessage("Error Occurred during processing")).type(responseType).build();
        }
        return Response.status(Response.Status.OK).entity(output).type(responseType).build();
    }
}
