# ooredoo-pm4ml-core-connector

Most of the content details and instructions about development and deployment can be found into
the main [template project](https://github.com/pm4ml/template-rest-pm4ml-core-connector).
Additional or different topics specified below.

### Overwrite application properties

To run application and specify the proper credentials for DFSP API connection
(it is not required specify all the fields if it isn't used):
```
java \
-Dml-conn.outbound.host="http://localhost:4001" \
-Ddfsp.host="https://dfsp/api" \
-Ddfsp.api-key="key" \
-Ddfsp.agent.mobile-number="1234" \
-Ddfsp.agent.mpin="1234" \
-Ddfsp.agent.tpin="1234" \
-Ddfsp.product-id="1234" \
-jar ./core-connector/target/core-connector.jar
```
```
docker run --rm \
-e MLCONN_OUTBOUND_ENDPOINT="http://localhost:4001" \
-e DFSP_HOST="https://dfsp/api" \
-e DFSP_AUTH_API_KEY="key" \
-e DFSP_AGENT_MOB_NUM="1234" \
-e DFSP_AGENT_MPIN="1234" \
-e DFSP_AGENT_TPIN="1234" \
-e DFSP_PRODUCT_ID="1234" \
-p 3003:3003 core-connector:latest
```
**NOTE:** keep the values in double quotes (") and scape any special character (\\@).