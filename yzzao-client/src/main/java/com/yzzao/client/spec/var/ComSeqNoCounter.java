package com.yzzao.client.spec.var;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 通讯序号计数器:0-65535
 * @author kangtengjiao
 * @since 2018-10-04
 */
public final class ComSeqNoCounter {
	/**原子变量*/
	private static final AtomicInteger counter = new AtomicInteger(0x00000000);

	public static int getAndIncrement() {
		return counter.getAndIncrement() & 0x0000ffff;
	}
	public static int get() {
		return counter.get() & 0x0000ffff;
	}
}
