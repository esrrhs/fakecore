package com.github.esrrhs.fakecore.net;

/**
 * Created by esrrhs on 2018/2/7.
 */
public interface ProcessFunc<T>
{
    void process(T message);
}
