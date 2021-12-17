package com.modusbox.client.router;

import com.modusbox.client.exception.RouteExceptionHandlingConfigurer;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;

public class TransfersRouter extends RouteBuilder {

    private static final String ROUTE_ID_POST = "com.modusbox.postTransfers";
    private static final String ROUTE_ID_PUT = "com.modusbox.putTransfersByTransferId";
    private static final String ROUTE_ID_GET = "com.modusbox.getTransfersByTransferId";
    private static final String COUNTER_NAME_POST = "counter_post_transfers_requests";
    private static final String COUNTER_NAME_PUT = "counter_put_transfers_requests";
    private static final String COUNTER_NAME_GET = "counter_get_transfers_requests";
    private static final String TIMER_NAME_POST = "histogram_post_transfers_timer";
    private static final String TIMER_NAME_PUT = "histogram_put_transfers_timer";
    private static final String TIMER_NAME_GET = "histogram_get_transfers_timer";
    private static final String HISTOGRAM_NAME_POST = "histogram_post_transfers_requests_latency";
    private static final String HISTOGRAM_NAME_PUT = "histogram_put_transfers_requests_latency";
    private static final String HISTOGRAM_NAME_GET = "histogram_get_transfers_requests_latency";

    public static final Counter requestCounterPost = Counter.build()
            .name(COUNTER_NAME_POST)
            .help("Total requests for POST /transfers.")
            .register();

    private static final Histogram requestLatencyPost = Histogram.build()
            .name(HISTOGRAM_NAME_POST)
            .help("Request latency in seconds for POST /transfers.")
            .register();

    public static final Counter requestCounterPut = Counter.build()
            .name(COUNTER_NAME_PUT)
            .help("Total requests for PUT /transfers/{transferId}.")
            .register();

    private static final Histogram requestLatencyPut = Histogram.build()
            .name(HISTOGRAM_NAME_PUT)
            .help("Request latency in seconds for PUT /transfers/{transferId}.")
            .register();

    public static final Counter requestCounterGet = Counter.build()
            .name(COUNTER_NAME_GET)
            .help("Total requests for GET /transfers/{transferId}.")
            .register();

    private static final Histogram requestLatencyGet = Histogram.build()
            .name(HISTOGRAM_NAME_GET)
            .help("Request latency in seconds for GET /transfers/{transferId}.")
            .register();

    private final RouteExceptionHandlingConfigurer exception = new RouteExceptionHandlingConfigurer();

    public void configure() {

        // Add custom global exception handling strategy
        exception.configureExceptionHandling(this);

        from("direct:postTransfers").routeId(ROUTE_ID_POST).doTry()
                .process(exchange -> {
                    requestCounterPost.inc(1); // increment Prometheus Counter metric
                    exchange.setProperty(TIMER_NAME_POST, requestLatencyPost.startTimer()); // initiate Prometheus Histogram metric
                })
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Request received, " + ROUTE_ID_POST + "', null, null, 'Input Payload: ${body}')") // default logging
                /*
                 * BEGIN processing
                 */
                .setBody(constant("{\"homeTransactionId\": \"1234\"}"))
                /*
                 * END processing
                 */
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Send response, " + ROUTE_ID_POST + "', null, null, 'Output Payload: ${body}')") // default logging
                .doFinally().process(exchange -> {
                    ((Histogram.Timer) exchange.getProperty(TIMER_NAME_POST)).observeDuration(); // stop Prometheus Histogram metric
                }).end()
        ;

        from("direct:putTransfersByTransferId").routeId(ROUTE_ID_PUT).doTry()
                .process(exchange -> {
                    requestCounterPut.inc(1); // increment Prometheus Counter metric
                    exchange.setProperty(TIMER_NAME_PUT, requestLatencyPut.startTimer()); // initiate Prometheus Histogram metric
                })
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Request received, PUT /transfers/${header.transferId}', " +
                        "null, null, null)")
                /*
                 * BEGIN processing
                 */

                .setProperty("origPayload", simple("${body}"))
                .setProperty("idValue", simple("{{dfsp.agent.mobile-number}}"))
                .setProperty("mpin", simple("{{dfsp.agent.mpin}}"))
                .setProperty("tpin", simple("{{dfsp.agent.tpin}}"))
                .setProperty("productId", simple("{{dfsp.product-id}}"))

                .to("direct:getToken")

                .marshal().json()
                .transform(datasonnet("resource:classpath:mappings/putTransfersRequest.ds"))
                .setBody(simple("${body.content}"))
                .marshal().json()

                .removeHeaders("CamelHttp*")
                .removeHeader(Exchange.HTTP_URI)
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader("Content-Type", constant("application/json"))
                .setHeader("Authorization", simple("Bearer ${exchangeProperty.authToken}"))

                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Calling Ooredoo API, postTransaction, " +
                        "POST {{dfsp.host}}/modusboxNonFinancialXMLService/1.0.0', " +
                        "'Tracking the request', 'Track the response', 'Input Payload: ${body}')")
                .toD("{{dfsp.host}}/modusboxNonFinancialXMLService/1.0.0?bridgeEndpoint=true")
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Response from Ooredoo API, postTransaction: ${body}', " +
                        "'Tracking the response', 'Verify the response', null)")

                .transform(datasonnet("resource:classpath:mappings/putTransfersResponse.ds"))
                .setBody(simple("${body.content}"))

                .to("direct:choiceRoute")

                /*
                 * END processing
                 */
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Final Response: ${body}', " +
                        "null, null, 'Response of PUT /transfers/${header.transferId} API')")
                .doFinally().process(exchange -> {
                    ((Histogram.Timer) exchange.getProperty(TIMER_NAME_PUT)).observeDuration(); // stop Prometheus Histogram metric
                }).end()
        ;

        from("direct:choiceRoute")
                .choice()
                    .when(simple("${body.size} != 0"))
                        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
                    .otherwise()
                        .setBody(constant(""))
                .endChoice()
        ;

        from("direct:getTransfersByTransferId").routeId(ROUTE_ID_GET).doTry()
                .process(exchange -> {
                    requestCounterGet.inc(1); // increment Prometheus Counter metric
                    exchange.setProperty(TIMER_NAME_GET, requestLatencyGet.startTimer()); // initiate Prometheus Histogram metric
                })
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Request received, GET /transfers/${header.transferId}', " +
                        "null, null, null)")
                /*
                 * BEGIN processing
                 */

                .removeHeaders("CamelHttp*")
                .setHeader("Content-Type", constant("application/json"))
                .setHeader("Accept", constant("application/json"))
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))

                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Calling Hub API, get transfers, GET {{dfsp.host}}', " +
                        "'Tracking the request', 'Track the response', 'Input Payload: ${body}')")
                .toD("{{ml-conn.outbound.host}}/transfers/${header.transferId}?bridgeEndpoint=true&throwExceptionOnFailure=false")
                .unmarshal().json()
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Response from Hub API, get transfers: ${body}', " +
                        "'Tracking the response', 'Verify the response', null)")
//                .process(exchange -> System.out.println())

                .marshal().json()
                .transform(datasonnet("resource:classpath:mappings/getTransfersResponse.ds"))
                .setBody(simple("${body.content}"))
                .marshal().json()

                /*
                 * END processing
                 */
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Final Response: ${body}', " +
                        "null, null, 'Response of GET /transfers/${header.transferId} API')")

                .doFinally().process(exchange -> {
            ((Histogram.Timer) exchange.getProperty(TIMER_NAME_GET)).observeDuration(); // stop Prometheus Histogram metric
        }).end()
        ;

    }
}
