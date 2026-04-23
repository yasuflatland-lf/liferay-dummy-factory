package com.liferay.support.tools.utils;

import com.liferay.portal.kernel.transaction.Propagation;
import com.liferay.portal.kernel.transaction.TransactionConfig;
import com.liferay.portal.kernel.transaction.TransactionInvokerUtil;

import java.util.concurrent.Callable;

/**
 * Static helper that wraps {@link TransactionInvokerUtil#invoke} with the
 * single transaction configuration used by every Creator in this module
 * ({@code Propagation.REQUIRED} + rollback on {@code Exception.class}).
 *
 * Creators call {@link #run(Callable)} instead of inlining a local
 * {@code _transactionConfig} field and the {@code TransactionInvokerUtil}
 * call. This centralises the transaction contract in one place so that a
 * future change (retry, metrics, MDC logging) can be applied without
 * editing every Creator.
 */
public final class BatchTransaction {

	public static <T> T run(Callable<T> callable) throws Throwable {
		return TransactionInvokerUtil.invoke(_CONFIG, callable);
	}

	private BatchTransaction() {
	}

	private static final TransactionConfig _CONFIG =
		TransactionConfig.Factory.create(
			Propagation.REQUIRED, new Class<?>[] {Exception.class});

}
