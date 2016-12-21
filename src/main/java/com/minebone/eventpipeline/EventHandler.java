package com.minebone.eventpipeline;

import org.bukkit.event.Event;

public interface EventHandler<T extends Event> {

    void listen(T event);

}
