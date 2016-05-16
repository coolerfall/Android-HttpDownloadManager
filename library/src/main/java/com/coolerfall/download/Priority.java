package com.coolerfall.download;

/**
 * Download request will be processed from higher priorities to lower priorities.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
public enum Priority {
	/**
	 * The lowest priority.
	 */
	LOW,
	/**
	 * Normal priority(default).
	 */
	NORMAL,
	/**
	 * The highest priority.
	 */
	HIGH,
}
