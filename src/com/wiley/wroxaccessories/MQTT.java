package com.wiley.wroxaccessories;

import java.io.ByteArrayOutputStream;

public class MQTT {
	protected static byte VERSION = (byte) 0x01;
	protected static String PROTOCOL_NAME = "P2PMQTT";
	protected static final int CONNECT = 1;
	protected static final int CONNACK = 2;
	protected static final int PUBLISH = 3;
	protected static final int PUBACK = 4;
	protected static final int PUBREC = 5;
	protected static final int PUBREL = 6;
	protected static final int PUBCOMP = 7;
	protected static final int SUBSCRIBE = 8;
	protected static final int SUBACK = 9;
	protected static final int UNSUBSCRIBE = 10;
	protected static final int UNSUBACK = 11;
	protected static final int PINGREQ = 12;
	protected static final int PINGRESP = 13;
	protected static final int DISCONNECT = 14;

	protected static byte[] encode(int type, boolean retain, int qos,
			boolean dup, byte[] payload, String... params) {
		ByteArrayOutputStream mqtt = new ByteArrayOutputStream();
		mqtt.write((byte) ((retain ? 1 : 0) | qos << 1 | (dup ? 1 : 0) << 3 | type << 4));
		ByteArrayOutputStream variableHeader = new ByteArrayOutputStream();
		switch (type) {
		case CONNECT:
			boolean username = Boolean.parseBoolean(params[0]);
			boolean password = Boolean.parseBoolean(params[1]);
			boolean will = Boolean.parseBoolean(params[2]);
			boolean will_retain = Boolean.parseBoolean(params[3]);
			boolean cleansession = Boolean.parseBoolean(params[4]);
			variableHeader.write(0x00);
			variableHeader.write(PROTOCOL_NAME.getBytes("UTF-8").length);
			variableHeader.write(PROTOCOL_NAME.getBytes("UTF-8"));
			variableHeader.write(VERSION);
			variableHeader.write((cleansession ? 1 : 0) << 1
					| (will ? 1 : 0) << 2 | (qos) << 3
					| (will_retain ? 1 : 0) << 5 | (password ? 1 : 0) << 6
					| (username ? 1 : 0) << 7);
			variableHeader.write(0x00);
			variableHeader.write(0x000A);
			break;
		case PUBLISH:
			int message_id = Integer.parseInt(params[0]);
			String topic_name = params[1];
			variableHeader.write(0x00);
			variableHeader.write(topic_name.getBytes("UTF-8").length);
			variableHeader.write(topic_name.getBytes("UTF-8"));
			break;
		case SUBSCRIBE:
			message_id = Integer.parseInt(params[0]);
			variableHeader.write((message_id >> 8) & 0xFF);
			variableHeader.write(message_id & 0xFF);
			break;
		}
		int length = payload.length + variableHeader.size();
		do {
			byte digit = (byte) (length % 128);
			length /= 128;
			if (length > 0)
				digit = (byte) (digit | 0x80);
			mqtt.write(digit);
		} while (length > 0);
		mqtt.write(payload);
		return mqtt.toByteArray();
	}

	public static MQTTMessage decode(final byte[] message) {
		int i = 0;
		MQTTMessage mqtt = new MQTTMessage();
		mqtt.type = (message[i] >> 4) & 0x0F;
		mqtt.DUP = ((message[i] >> 3) & 0x01) == 0 ? false : true;
		mqtt.QoS = (message[i] >> 1) & 0x03;
		mqtt.retain = (message[i] & 0x01) == 0 ? false : true;
		i++;
		int multiplier = 1;
		int len = 0;
		byte digit = 0;
		do {
			digit = message[i++];
			len += (digit & 127) * multiplier;
			multiplier *= 128;
		} while ((digit & 128) != 0);
		mqtt.remainingLength = len;
		switch (mqtt.type) {
		case CONNECT:
			int protocol_name_len = (message[i++] << 8 | message[i++]);
			mqtt.variableHeader.put("protocol_name", new String(message, i,
					protocol_name_len));
			mqtt.variableHeader.put("protocol_version", message[i++]);
			mqtt.variableHeader.put("has_username",
					((message[i++] << 7) & 0x01) == 0 ? false : true);
			mqtt.variableHeader.put("has_password",
					((message[i] << 6) & 0x01) == 0 ? false : true);
			mqtt.variableHeader.put("will_retain",
					((message[i] << 5) & 0x01) == 0 ? false : true);
			mqtt.variableHeader.put("will_qos", ((message[i] << 3) & 0x03));
			mqtt.variableHeader.put("will",
					((message[i] << 2) & 0x01) == 0 ? false : true);
			mqtt.variableHeader.put("clean_session",
					((message[i] << 1) & 0x01) == 0 ? false : true);
			int keep_alive_len = (message[i++] << 8 | message[i++]);
			mqtt.variableHeader.put("keep_alive", new String(message, i,
					keep_alive_len));
			break;
		case PUBLISH:
			int topic_name_len = (message[i++] << 8 | message[i++]);
			mqtt.variableHeader.put("topic_name", new String(message, i,
					topic_name_len));
			mqtt.variableHeader.put("message_id",
					(message[i++] << 8 | message[i++]));
			break;
		case SUBSCRIBE:
			mqtt.variableHeader.put("message_id",
					(message[i++] << 8 | message[i++]));
			break;
		case PINGREQ:
			// PINGREQ has no variable header
			break;
		}
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		for (int b = i; b < message.length; b++)
			payload.write(message[b]);
		mqtt.payload = payload.toByteArray();
		return mqtt;
	}
}