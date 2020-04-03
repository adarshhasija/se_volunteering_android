package com.starsearth.five.deserializers;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.starsearth.five.domain.Task;

import java.lang.reflect.Type;

public class TypeDeserializer implements
        JsonDeserializer<Task.Type> {


    @Override
    public Task.Type deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        int typeInt = json.getAsInt();
        return Task.Type.fromInt(typeInt);
    }
}
