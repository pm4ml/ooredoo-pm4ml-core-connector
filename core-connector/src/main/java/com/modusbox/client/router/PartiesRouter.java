package com.modusbox.client.router;

import com.modusbox.client.exception.RouteExceptionHandlingConfigurer;
import com.modusbox.client.processor.ProcessPartiesResponse;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;

public class PartiesRouter extends RouteBuilder {

    private static final String ROUTE_ID = "com.modusbox.getParties";
    private static final String COUNTER_NAME = "counter_get_parties_requests";
	private static final String TIMER_NAME = "histogram_get_parties_timer";
	private static final String HISTOGRAM_NAME = "histogram_get_parties_requests_latency";

    public static final Counter requestCounter = Counter.build()
            .name(COUNTER_NAME)
            .help("Total requests for GET /parties.")
            .register();

    private static final Histogram requestLatency = Histogram.build()
            .name(HISTOGRAM_NAME)
            .help("Request latency in seconds for GET /parties.")
            .register();

    private final RouteExceptionHandlingConfigurer exception = new RouteExceptionHandlingConfigurer();
    private ProcessPartiesResponse processPartiesResponse = new ProcessPartiesResponse();

    public void configure() {

        // Add custom global exception handling strategy
        exception.configureExceptionHandling(this);

        from("direct:getPartiesByIdTypeIdValue").routeId(ROUTE_ID).doTry()
                .process(exchange -> {
                    requestCounter.inc(1); // increment Prometheus Counter metric
                    exchange.setProperty(TIMER_NAME, requestLatency.startTimer()); // initiate Prometheus Histogram metric
                })
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Request received, " + ROUTE_ID + "', null, null, null)") // default logging
                /*
                 * BEGIN processing
                 */
                .setBody(simple("{}"))
                .removeHeaders("CamelHttp*")

                .to("direct:getToken")

                .removeHeader(Exchange.HTTP_URI)
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader("Content-Type", constant("application/json"))
                .setHeader("Authorization", simple("Bearer ${exchangeProperty.authToken}"))

                .setBody(simple("<COMMAND>\n" +
                        "<TYPE>USERINFOREQ</TYPE>\n" +
                        "<IDENTIFICATION>${header.idValue}</IDENTIFICATION>\n" +
                        "<LEVEL>HIGH</LEVEL>\n" +
                        "<INPUTTYPE>MSISDN</INPUTTYPE>\n" +
                        "</COMMAND>"))

                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Calling Ooredoo API, getToken', " +
                        "'Tracking the request', 'Track the response', " +
                        "'Request sent to, POST {{dfsp.host}}/modusboxNonFinancialXMLService/1.0.0')")

                // Call login API which also returns customer name base on MSISDN
                .toD("{{dfsp.host}}/modusboxNonFinancialXMLService/1.0.0?bridgeEndpoint=true")
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Response from Ooredoo API, getParties: ${body}', " +
                        "'Tracking the response', 'Verify the response', null)")

                .removeHeader("Content-Type")
                .process(processPartiesResponse)

                /*.marshal().json()
                .transform(datasonnet("resource:classpath:mappings/getPartiesResponse.ds"))
                .setBody(simple("${body.content}"))
                .unmarshal().json()*/

                /*
                 * END processing
                 */
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Send response, " + ROUTE_ID + "', null, null, 'Output Payload: ${body}')") // default logging
                .doFinally().process(exchange -> {
					((Histogram.Timer) exchange.getProperty(TIMER_NAME)).observeDuration(); // stop Prometheus Histogram metric
				}).end()
        ;

        from("direct:getToken").routeId("com.modusbox.getToken")
                .removeHeaders("CamelHttp*")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader("Content-Type", constant("application/x-www-form-urlencoded"))
                .setHeader("Authorization", simple("{{dfsp.api-key}}"))

                .setBody(constant("grant_type=client_credentials"))

                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Calling Ooredoo API, getToken', " +
                        "'Tracking the request', 'Track the response', " +
                        "'Request sent to, POST {{dfsp.host}}/token')")
                // Call login API which also returns customer name base on MSISDN
                .toD("{{dfsp.host}}/token?bridgeEndpoint=true")
                .unmarshal().json()

                // Extract citizens auth token from response
                .setProperty("authToken", simple("${body['access_token']}"))

                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Auth token loaded', null, null, null)") // default logging
        ;
    }
}