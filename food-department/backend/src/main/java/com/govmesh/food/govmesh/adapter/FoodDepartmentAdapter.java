package com.govmesh.food.govmesh.adapter;

import com.govmesh.food.govmesh.config.SoapSimulationConfig;
import com.govmesh.food.govmesh.dto.CanonicalAddressUpdateRequest;
import com.govmesh.food.govmesh.dto.CanonicalAddressUpdateResponse;
import com.govmesh.food.govmesh.mapper.FoodDepartmentSchemaMapper;
import com.govmesh.food.soap.dto.UpdateRationAddress;
import com.govmesh.food.soap.dto.UpdateRationAddressResponse;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.SoapFaultClientException;

@Component
public class FoodDepartmentAdapter {

    private final FoodDepartmentSchemaMapper schemaMapper;
    private final WebServiceTemplate webServiceTemplate;
    private final SoapSimulationConfig simulationConfig;

    public FoodDepartmentAdapter(FoodDepartmentSchemaMapper schemaMapper) {
        this(schemaMapper, new SoapSimulationConfig("SUCCESS"));
    }

    @org.springframework.beans.factory.annotation.Autowired
    public FoodDepartmentAdapter(FoodDepartmentSchemaMapper schemaMapper, SoapSimulationConfig simulationConfig) {
        this.schemaMapper = schemaMapper;
        this.simulationConfig = simulationConfig;
        this.webServiceTemplate = createWebServiceTemplate();
    }

    public CanonicalAddressUpdateResponse sendAddressUpdate(CanonicalAddressUpdateRequest canonicalRequest, String soapEndpointUrl) {
        // Dev/Demo failure simulation check
        if (simulationConfig != null) {
            SoapSimulationConfig.SimulationMode mode = simulationConfig.getMode();
            if (mode == SoapSimulationConfig.SimulationMode.TIMEOUT) {
                return CanonicalAddressUpdateResponse.builder()
                        .applicationId(canonicalRequest.getApplicationId())
                        .status("FAILED")
                        .message("Food Department SOAP service timed out while waiting for response.")
                        .correlationId(canonicalRequest.getCorrelationId())
                        .targetDepartment("FOOD")
                        .errorCode("TIMEOUT")
                        .build();
            } else if (mode == SoapSimulationConfig.SimulationMode.SERVICE_UNAVAILABLE) {
                return CanonicalAddressUpdateResponse.builder()
                        .applicationId(canonicalRequest.getApplicationId())
                        .status("FAILED")
                        .message("Food Department SOAP service is temporarily unavailable.")
                        .correlationId(canonicalRequest.getCorrelationId())
                        .targetDepartment("FOOD")
                        .errorCode("SERVICE_UNAVAILABLE")
                        .build();
            }
        }

        UpdateRationAddress soapRequest = schemaMapper.mapCanonicalToSoapRequest(canonicalRequest);

        try {
            Object responseObj = webServiceTemplate.marshalSendAndReceive(soapEndpointUrl, soapRequest);

            if (responseObj instanceof UpdateRationAddressResponse soapResponse) {
                return CanonicalAddressUpdateResponse.builder()
                        .applicationId(soapResponse.getApplicationId())
                        .status(soapResponse.getStatus())
                        .message(soapResponse.getMessage())
                        .correlationId(soapResponse.getCorrelationId())
                        .targetDepartment("FOOD")
                        .build();
            } else {
                return CanonicalAddressUpdateResponse.builder()
                        .applicationId(canonicalRequest.getApplicationId())
                        .status("FAILED")
                        .message("Unexpected response type received from Food SOAP service.")
                        .correlationId(canonicalRequest.getCorrelationId())
                        .targetDepartment("FOOD")
                        .errorCode("UNEXPECTED_RESPONSE")
                        .build();
            }
        } catch (SoapFaultClientException soapFaultEx) {
            String faultMsg = soapFaultEx.getFaultStringOrReason();
            String errorCode = "SOAP_FAULT";

            if (faultMsg != null && faultMsg.contains(":")) {
                String[] parts = faultMsg.split(":", 2);
                errorCode = parts[0].trim();
                faultMsg = parts[1].trim();
            }

            return CanonicalAddressUpdateResponse.builder()
                    .applicationId(canonicalRequest.getApplicationId())
                    .status("FAILED")
                    .message(faultMsg)
                    .correlationId(canonicalRequest.getCorrelationId())
                    .targetDepartment("FOOD")
                    .errorCode(errorCode)
                    .build();
        } catch (Exception ex) {
            return CanonicalAddressUpdateResponse.builder()
                    .applicationId(canonicalRequest.getApplicationId())
                    .status("FAILED")
                    .message("Failed to communicate with Food Department SOAP service: " + ex.getMessage())
                    .correlationId(canonicalRequest.getCorrelationId())
                    .targetDepartment("FOOD")
                    .errorCode("SERVICE_UNAVAILABLE")
                    .build();
        }
    }

    private WebServiceTemplate createWebServiceTemplate() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("com.govmesh.food.soap.dto");

        try {
            marshaller.afterPropertiesSet();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize Jaxb2Marshaller for FoodDepartmentAdapter", e);
        }

        WebServiceTemplate template = new WebServiceTemplate();
        template.setMarshaller(marshaller);
        template.setUnmarshaller(marshaller);
        return template;
    }
}
