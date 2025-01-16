package io.fares.bind.kscharc.tf;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.msgpack.jackson.dataformat.MessagePackFactory;

import java.io.IOException;


public class MessagePackMapper {

  private static final ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory());

  public static <T> byte[] serialize(T object) throws IOException {
    return objectMapper.writeValueAsBytes(object);
  }

  public static <T> T deserialize(byte[] data, Class<T> clazz) throws IOException {
    return objectMapper.readValue(data, clazz);
  }

}
