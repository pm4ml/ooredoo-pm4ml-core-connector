package com.modusbox.client.jaxrs;

import com.modusbox.client.api.PartiesApi;
import com.modusbox.client.model.TransferPartyInbound;

import javax.validation.constraints.Size;

public class PartiesApiImpl implements PartiesApi {

    @Override
    public TransferPartyInbound getPartiesByIdTypeIdValue(String idType, @Size(min = 1, max = 128) String idValue) {
        return null;
    }

    @Override
    public TransferPartyInbound getPartiesByIdTypeIdValueIdSubValue(String idType, @Size(min = 1, max = 128) String idValue, @Size(min = 1, max = 128) String idSubValue) {
        return null;
    }
}
