package com.github.esrrhs.fakecore.net;

/**
 * Created by esrrhs on 2018/2/7.
 */
public class ProcessorPool<T>
{
	private Processor<T>[] processors;

	public ProcessorPool(int size, int queueSize, ProcessFunc<T> processFunc)
	{
		this.processors = new Processor[size];
		for (int i = 0; i < size; i++)
		{
			this.processors[i] = new Processor<T>(queueSize, processFunc);
		}
		for (int i = 0; i < size; i++)
		{
			this.processors[i].start();
		}
	}

	public Processor<T>[] getProcessors()
	{
		return processors;
	}

	public int getProcessorsSize()
	{
		return processors.length;
	}

	public void putMsg(int hashCode, T message)
	{
		int index = Math.abs(hashCode) % getProcessorsSize();
		getProcessors()[index].putMsg(message);
	}

}
