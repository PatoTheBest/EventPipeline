package com.minebone.eventpipeline;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.event.*;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class EventPipeline<T extends Event> implements Listener, EventExecutor {

    private static final Map<Class<? extends Event>, EventPipeline<? extends Event>> map = new HashMap<>();
    protected static JavaPlugin javaPlugin;

    private EventHandlerContext<T> baseContext;
    protected Class<T> eventClass;

    private EventPipeline(Class<T> eventClass) {
        this.eventClass = eventClass;
    }

    @SuppressWarnings("unchecked")
    public static <E extends Event> EventPipeline<E> getPipeline(Class<E> eventClass) {
        if (map.containsKey(eventClass)) {
            if(!map.get(eventClass).eventClass.getName().equals(eventClass.getName())) {
                throw new RuntimeException(map.get(eventClass).eventClass.getSimpleName() + " is not an instance of " + eventClass.getSimpleName());
            }
            return (EventPipeline<E>) map.get(eventClass);
        }

        EventPipeline<E> eventPipeline = new EventPipeline<>(eventClass);
        Bukkit.getServer().getPluginManager().registerEvent(eventClass, eventPipeline, EventPriority.MONITOR, eventPipeline, EventPipeline.javaPlugin);
        map.put(eventClass, eventPipeline);
        return eventPipeline;
    }

    public static void setBasePlugin(JavaPlugin javaPlugin) {
        EventPipeline.javaPlugin = javaPlugin;
    }

    public EventPipeline<T> addFirst(String name, EventHandler<T> eventHandler) {
        Validate.notNull(name, "Context name cannot be null");

        EventHandlerContext<T> eventHandlerContext = newContext(name, eventHandler);

        if(baseContext != null) {
            baseContext.previous = eventHandlerContext;
            eventHandlerContext.next = baseContext;
        }

        baseContext = eventHandlerContext;
        return this;
    }

    public EventPipeline<T> addLast(String name, EventHandler<T> eventHandler) {
        Validate.notNull(name, "Context name cannot be null");

        EventHandlerContext<T> eventHandlerContext = newContext(name, eventHandler);
        EventHandlerContext<T> last = lastContext();

        last.next = eventHandlerContext;
        eventHandlerContext.previous = last;
        return this;
    }

    public EventPipeline<T> addBefore(String baseName, String name, EventHandler<T> eventHandler) {
        Validate.notNull(baseName, "Base context name cannot be null");
        Validate.notNull(name, "Context name cannot be null");

        EventHandlerContext<T> eventHandlerContext = context(baseName);
        if(eventHandlerContext == null) {
            throw new NoSuchElementException("Base context cannot be found");
        }

        if(eventHandlerContext == baseContext) {
            return addFirst(name, eventHandler);
        }

        EventHandlerContext<T> newContext = newContext(name, eventHandler);

        EventHandlerContext<T> prevContext = eventHandlerContext.previous;
        prevContext.next = newContext;
        newContext.previous = prevContext;
        newContext.next = eventHandlerContext;
        eventHandlerContext.previous = newContext;
        return this;
    }

    public EventPipeline<T> addAfter(String baseName, String name, EventHandler<T> eventHandler) {
        Validate.notNull(baseName, "Base context name cannot be null");
        Validate.notNull(name, "Context name cannot be null");

        EventHandlerContext<T> eventHandlerContext = context(baseName);
        if(eventHandlerContext == null) {
            throw new NoSuchElementException("Base context cannot be found");
        }

        if(eventHandlerContext == lastContext()) {
            return addLast(name, eventHandler);
        }

        EventHandlerContext<T> newContext = newContext(name, eventHandler);
        EventHandlerContext<T> nextContext = eventHandlerContext.next;

        eventHandlerContext.next = newContext;
        newContext.previous = eventHandlerContext;
        newContext.next = nextContext;
        newContext.previous = newContext;
        return this;
    }

    public EventHandlerContext<T> context(String name) {
        Validate.notNull(name, "Context name cannot be null");
        validateBaseContext();

        EventHandlerContext<T> next = baseContext;
        while(next != null) {
            if(next.name.equalsIgnoreCase(name)) {
                return next;
            }

            next = next.next;
        }

        return null;
    }

    public EventPipeline<T> remove(String name) {
        EventHandlerContext<T> contextToRemove = context(name);

        if(contextToRemove == null) {
            throw new NoSuchElementException("Context cannot be found");
        }

        return remove(contextToRemove);
    }

    public EventPipeline<T> remove(EventHandlerContext<T> contextToRemove) {
        EventHandlerContext<T> prevContext = contextToRemove.previous;
        EventHandlerContext<T> nextContext = contextToRemove.next;

        prevContext.next = nextContext;

        if(nextContext != null) {
            nextContext.previous = prevContext;
        }

        return this;
    }

    public EventPipeline<T> removeFirst() {
        return remove(firstContext());
    }

    public EventPipeline<T> removeLast() {
        return remove(lastContext());
    }

    public EventPipeline<T> replace(String oldContextName, String newContextName, EventHandler<T> eventHandler) {
        EventHandlerContext<T> newContext = newContext(newContextName, eventHandler);
        return replace(oldContextName, newContext);
    }

    public EventPipeline<T> replace(String oldContextName, EventHandlerContext<T> newContext) {
        EventHandlerContext<T> oldContext = context(oldContextName);
        return replace(oldContext, newContext);
    }

    public EventPipeline<T> replace(EventHandlerContext<T> oldContext, EventHandlerContext<T> newContext) {
        EventHandlerContext<T> prevContext = oldContext.previous;
        EventHandlerContext<T> nextContext = oldContext.next;

        prevContext.next = nextContext;
        newContext.previous = prevContext;
        newContext.next = nextContext;


        if(nextContext != null) {
            nextContext.previous = newContext;
        }
        return this;
    }

    public EventHandler<T> first() {
        return firstContext().eventHandler();
    }

    public EventHandlerContext<T> firstContext() {
        return baseContext;
    }

    public EventHandler<T> last() {
        return lastContext().eventHandler();
    }

    public EventHandlerContext<T> lastContext() {
        EventHandlerContext<T> last = baseContext;

        while(last.next != null) {
            last = last.next;
        }

        return last;
    }

    public Map<String, EventHandlerContext<T>> toMap() {
        Map<String, EventHandlerContext<T>> map = new HashMap<>();

        EventHandlerContext<T> current = baseContext;
        while(current != null) {
            map.put(current.name, current);
            current = current.next;
        }

        return map;
    }

    public List<String> names() {
        return new ArrayList<>(toMap().keySet());
    }

    private EventHandlerContext<T> newContext(String name, EventHandler<T> eventHandler) {
        Validate.notNull(name, "Context name cannot be null");
        return new EventHandlerContext<>(name, this).eventHandler(eventHandler);
    }

    private void validateBaseContext() {
        if(baseContext == null) {
            throw new NoSuchElementException("No base context has been set");
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void execute(Listener listener, Event event) throws EventException {
        if(!eventClass.isInstance(event)) {
            throw new RuntimeException(event.getClass().getSimpleName() + " is not an instance of " + eventClass.getSimpleName());
        }

        if(event instanceof Cancellable) {
            Cancellable cancellableEvent = (Cancellable)event;
            EventHandlerContext<T> eventHandlerContext = baseContext;

            while (eventHandlerContext != null) {
                eventHandlerContext.eventHandler().listen((T) event);

                if(cancellableEvent.isCancelled()) {
                    return;
                }

                eventHandlerContext = eventHandlerContext.next;
            }
        } else {
            EventHandlerContext<T> eventHandlerContext = baseContext;

            while (eventHandlerContext != null) {
                eventHandlerContext.eventHandler().listen((T) event);

                eventHandlerContext = eventHandlerContext.next;
            }
        }
    }
}
