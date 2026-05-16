package com.smelted.client.bot;

public enum MapBotState {
    STOPPED,
    ERROR,
    NEXT_POINT,
    PATHING,
    PLACING_FRAME,
    MOVING_CLOSER_TO_FRAME,
    WAITING_FOR_FRAME,
    WAITING_TO_INSERT_MAP,
    INSERTING_MAP_TEST,
    RELEASING_USE_KEY
}