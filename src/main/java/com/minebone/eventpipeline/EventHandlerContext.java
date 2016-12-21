package com.minebone.eventpipeline;

import org.bukkit.event.*;

public class EventHandlerContext<T extends Event> {

    private final EventPipeline<T> pipeline;
    private EventHandler<T> eventHandler;

    protected final String name;
    protected EventHandlerContext<T> next;
    protected EventHandlerContext<T> previous;

    public EventHandlerContext(String name, EventPipeline<T> pipeline) {
        this.name = name;
        this.pipeline = pipeline;
    }

    public EventHandlerContext<T> eventHandler(EventHandler<T> eventHandler) {
        this.eventHandler = eventHandler;
        return this;
    }

    public EventHandler<T> eventHandler() {
        return eventHandler;
    }

}
