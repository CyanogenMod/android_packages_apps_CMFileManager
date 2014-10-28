/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.zip;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

/**
 * Converts Java time values to DOS date/time values and vice versa.
 * This class has been introduced in order to enhance interoperability
 * between different flavours of the ZIP file format specification when
 * converting date/time from the serialized DOS format in a ZIP file to
 * the local system time, which is represented by a UNIX-like encoding
 * by the Java API.
 *
 * @author  Christian Schlichtherle
 */
public enum DateTimeConverter {

    /**
     * This instance applies the schedule for Daylight Saving Time (DST),
     * i.e. all time conversions will apply DST where appropriate to a
     * particular date.
     * <p>
     * This behaviour provides best interoperability with:
     * <ul>
     * <li>Java SE: {@code jar} utility
     *     and {@code java.util.zip} package</li>
     * <li>Info-ZIP: {@code unzip}</li>
     * </ul>
     */
    JAR {
        @Override
        GregorianCalendar getThreadLocalCalendar() {
            GregorianCalendar cal = calendar.get();
            if (null == cal) {
                cal = new GregorianCalendar();
                calendar.set(cal);
            }
            return cal;
        }

        @Override
        boolean roundUp(long jTime) {
            return false;
        }
    },

    /**
     * This instance ignores the schedule for Daylight Saving Time (DST),
     * i.e. all time conversions will use the same raw offset and current
     * DST savings, regardless of whether DST savings should be applied to
     * a particular date or not.
     * <p>
     * This behavior provides best interoperability with:
     * <ul>
     * <li>Windows Vista Explorer (as of June 30<sup>th</sup>, 2009)</li>
     * <li>WinZip 12.0</li>
     * <li>7-Zip 4.65</li>
     * </ul>
     */
    ZIP {
        @Override
        GregorianCalendar getThreadLocalCalendar() {
            TimeZone tz = TimeZone.getDefault();
            tz = new SimpleTimeZone(
                    // See http://java.net/jira/browse/TRUEZIP-191 .
                    tz.getOffset(System.currentTimeMillis()),
                    tz.getID());
            assert !tz.useDaylightTime();
            GregorianCalendar cal = calendar.get();
            if (null == cal) {
                cal = new GregorianCalendar(tz);
                calendar.set(cal);
            } else {
                // See http://java.net/jira/browse/TRUEZIP-281 .
                cal.setTimeZone(tz);
            }
            assert cal.isLenient();
            return cal;
        }

        @Override
        boolean roundUp(long jTime) {
            return true;
        }
    };

    final ThreadLocal<GregorianCalendar>
            calendar = new ThreadLocal<GregorianCalendar>();

    /**
     * Smallest supported DOS date/time value in a ZIP file,
     * which is January 1<sup>st</sup>, 1980 AD 00:00:00 local time.
     */
    static final long MIN_DOS_TIME = (1 << 21) | (1 << 16); // 0x210000;

    /**
     * Largest supported DOS date/time value in a ZIP file,
     * which is December 31<sup>st</sup>, 2107 AD 23:59:58 local time.
     */
    static final long MAX_DOS_TIME =
            ((long) (2107 - 1980) << 25)
            | (12 << 21)
            | (31 << 16)
            | (23 << 11)
            | (59 << 5)
            | (58 >> 1);

    /**
     * Returns whether the given Java time should be rounded up or down to the
     * next two second interval when converting it to a DOS date/time.
     *
     * @param  jTime The number of milliseconds since midnight, January 1st,
     *         1970 AD UTC (called <i>epoch</i> alias <i>Java time</i>).
     * @return {@code true} for round-up, {@code false} for round-down.
     */
    abstract boolean roundUp(long jTime);

    /**
     * Returns a thread local lenient gregorian calendar for date/time
     * conversion which has its timezone set according to the conventions of
     * the represented archive format.
     *
     * @return A thread local lenient gregorian calendar.
     */
    abstract GregorianCalendar getThreadLocalCalendar();

    /**
     * Converts a Java time value to a DOS date/time value.
     * <p>
     * If the given Java time value preceeds {@link #MIN_DOS_TIME},
     * then it's adjusted to this value.
     * If the given Java time value exceeds {@link #MAX_DOS_TIME},
     * then it's adjusted to this value.
     * <p>
     * The return value is rounded up or down to even seconds,
     * depending on {@link #roundUp}.
     *
     * @param  jtime The number of milliseconds since midnight, January 1st,
     *         1970 AD UTC (called <i>the epoch</i> alias Java time).
     * @return A DOS date/time value reflecting the local time zone and
     *         rounded down to even seconds
     *         and is in between {@link #MIN_DOS_TIME} and {@link #MAX_DOS_TIME}.
     * @throws IllegalArgumentException If {@code jTime} is negative.
     * @see    #toJavaTime(long)
     */
    final long toDosTime(final long jtime) {
        if (0 > jtime)
            throw new IllegalArgumentException("Negative Java time: " + jtime);
        final GregorianCalendar cal = getThreadLocalCalendar();
        cal.setTimeInMillis(roundUp(jtime) ? jtime + 1999 : jtime);
        long dtime = cal.get(Calendar.YEAR) - 1980;
        if (0 > dtime) return MIN_DOS_TIME;
        dtime = (dtime << 25)
                | ((cal.get(Calendar.MONTH) + 1) << 21)
                | (cal.get(Calendar.DAY_OF_MONTH) << 16)
                | (cal.get(Calendar.HOUR_OF_DAY) << 11)
                | (cal.get(Calendar.MINUTE) << 5)
                | (cal.get(Calendar.SECOND) >> 1);
        if (MAX_DOS_TIME < dtime) return MAX_DOS_TIME;
        assert MIN_DOS_TIME <= dtime && dtime <= MAX_DOS_TIME;
        return dtime;
    }

    /**
     * Converts a 32 bit integer encoded DOS date/time value to a Java time
     * value.
     * <p>
     * Note that not all 32 bit integers are valid DOS date/time values.
     * If an invalid DOS date/time value is provided, it gets adjusted by
     * overflowing the respective field value as if using a
     * {@link java.util.Calendar#setLenient lenient calendar}.
     * If the given DOS date/time value preceeds {@link #MIN_DOS_TIME},
     * then it's adjusted to this value.
     * If the given DOS date/time value exceeds {@link #MAX_DOS_TIME},
     * then it's adjusted to this value.
     * These features are provided in order to read bogus ZIP archive files
     * created by third party tools.
     * <p>
     * Note that the returned Java time may differ from its intended value at
     * the time of the creation of the ZIP archive file and when converting
     * it back again, the resulting DOS date/time value will not be the same as
     * {@code dTime}.
     * This is because of the limited resolution of two seconds for DOS
     * data/time values.
     *
     * @param  dtime The DOS date/time value.
     * @return The number of milliseconds since midnight, January 1st,
     *         1970 AD UTC (called <i>epoch</i> alias <i>Java time</i>)
     *         and is in between {@link #MIN_DOS_TIME} and {@link #MAX_DOS_TIME}.
     * @see    #toDosTime(long)
     */
    final long toJavaTime(long dtime) {
        if (MIN_DOS_TIME > dtime) dtime = MIN_DOS_TIME;
        else if (MAX_DOS_TIME < dtime) dtime = MAX_DOS_TIME;
        final int time = (int) dtime;
        final GregorianCalendar cal = getThreadLocalCalendar();
        cal.set(Calendar.ERA, GregorianCalendar.AD);
        cal.set(Calendar.YEAR, 1980 + ((time >> 25) & 0x7f));
        cal.set(Calendar.MONTH, ((time >> 21) & 0x0f) - 1);
        cal.set(Calendar.DAY_OF_MONTH, (time >> 16) & 0x1f);
        cal.set(Calendar.HOUR_OF_DAY, (time >> 11) & 0x1f);
        cal.set(Calendar.MINUTE, (time >> 5) & 0x3f);
        cal.set(Calendar.SECOND, (time << 1) & 0x3e);
        // DOS date/time has only two seconds granularity.
        // Make calendar return only total seconds in order to make this
        // work correctly.
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
}
