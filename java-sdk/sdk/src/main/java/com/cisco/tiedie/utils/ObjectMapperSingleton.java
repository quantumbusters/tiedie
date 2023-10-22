// Copyright (c) 2023, Cisco and/or its affiliates.
// All rights reserved.
// See license in distribution for details.

package com.cisco.tiedie.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;


public class ObjectMapperSingleton {
    private static ObjectMapper objectMapper;

    private ObjectMapperSingleton() {}

    public static ObjectMapper getInstance() {
        if (objectMapper != null) {
            return objectMapper;
        }

        objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        return objectMapper;
    }

}
