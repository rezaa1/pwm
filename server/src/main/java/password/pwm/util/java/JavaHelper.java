/*
 * Password Management Servlets (PWM)
 * http://www.pwm-project.org
 *
 * Copyright (c) 2006-2009 Novell, Inc.
 * Copyright (c) 2009-2021 The PWM Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package password.pwm.util.java;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.IOUtils;
import password.pwm.PwmConstants;
import password.pwm.http.bean.ImmutableByteArray;
import password.pwm.util.logging.PwmLogger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.management.LockInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class JavaHelper
{

    private static final PwmLogger LOGGER = PwmLogger.forClass( JavaHelper.class );

    private JavaHelper( )
    {
    }

    /**
     * Convert a byte[] array to readable string format. This makes the "hex" readable.
     *
     * @param in byte[] buffer to convert to string format
     * @return result String buffer in String format
     */
    @SuppressFBWarnings( "ICAST_QUESTIONABLE_UNSIGNED_RIGHT_SHIFT" )
    public static String byteArrayToHexString( final byte[] in )
    {
        final String[] pseudo =
                {
                        "0",
                        "1",
                        "2",
                        "3",
                        "4",
                        "5",
                        "6",
                        "7",
                        "8",
                        "9",
                        "A",
                        "B",
                        "C",
                        "D",
                        "E",
                        "F",
                        };

        if ( in == null || in.length <= 0 )
        {
            return "";
        }

        final StringBuilder out = new StringBuilder( in.length * 2 );

        for ( final byte b : in )
        {
            // strip off high nibble
            byte ch = ( byte ) ( b & 0xF0 );

            // shift the bits down
            ch = ( byte ) ( ch >>> 4 );

            // must do this is high order bit is on!
            ch = ( byte ) ( ch & 0x0F );

            // convert the nibble to a String Character
            out.append( pseudo[( int ) ch] );

            // strip off low nibble
            ch = ( byte ) ( b & 0x0F );

            // convert the nibble to a String Character
            out.append( pseudo[( int ) ch] );
        }

        return out.toString();
    }

    public static String binaryArrayToHex( final byte[] buf )
    {
        final char[] hexChars = "0123456789ABCDEF".toCharArray();
        final char[] chars = new char[2 * buf.length];
        for ( int i = 0; i < buf.length; ++i )
        {
            chars[2 * i] = hexChars[( buf[i] & 0xF0 ) >>> 4];
            chars[2 * i + 1] = hexChars[buf[i] & 0x0F];
        }
        return new String( chars );
    }

    public static <E extends Enum<E>> E readEnumFromString( final Class<E> enumClass, final E defaultValue, final String input )
    {
        return readEnumFromString( enumClass, input ).orElse( defaultValue );
    }

    public static <E extends Enum<E>> Optional<E> readEnumFromPredicate( final Class<E> enumClass, final Predicate<E> match )
    {
        if ( match == null )
        {
            return Optional.empty();
        }

        if ( enumClass == null || !enumClass.isEnum() )
        {
            return Optional.empty();
        }

        return EnumSet.allOf( enumClass ).stream().filter( match ).findFirst();
    }

    public static <E extends Enum<E>> Set<E> readEnumsFromPredicate( final Class<E> enumClass, final Predicate<E> match )
    {
        if ( match == null )
        {
            return Collections.emptySet();
        }

        if ( enumClass == null || !enumClass.isEnum() )
        {
            return Collections.emptySet();
        }

        return EnumSet.allOf( enumClass ).stream().filter( match ).collect( Collectors.toUnmodifiableSet() );
    }

    public static <E extends Enum<E>> Optional<E> readEnumFromString( final Class<E> enumClass, final String input )
    {
        if ( StringUtil.isEmpty( input ) )
        {
            return Optional.empty();
        }

        if ( enumClass == null || !enumClass.isEnum() )
        {
            return Optional.empty();
        }

        try
        {
            return Optional.of( Enum.valueOf( enumClass, input ) );
        }
        catch ( final IllegalArgumentException e )
        {
            /* noop */
            //LOGGER.trace("input=" + input + " does not exist in enumClass=" + enumClass.getSimpleName());
        }
        catch ( final Throwable e )
        {
            LOGGER.warn( () -> "unexpected error translating input=" + input + " to enumClass=" + enumClass.getSimpleName() + ", error: " + e.getMessage() );
        }

        return Optional.empty();
    }

    public static String throwableToString( final Throwable throwable )
    {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter( sw );
        throwable.printStackTrace( pw );
        pw.flush();
        return sw.toString();
    }

    /**
     * Converts an exception to a string message.  Handles cases where the message in the exception is null
     * and/or there are multiple nested cause exceptions.
     *
     * @param e The exception to convert to a string
     * @return A string containing any meaningful extractable cause information, suitable for debugging.
     */
    public static String readHostileExceptionMessage( final Throwable e )
    {
        final StringBuilder errorMsg = new StringBuilder();
        errorMsg.append( e.getClass().getName() );
        if ( e.getMessage() != null )
        {
            errorMsg.append( ": " ).append( e.getMessage() );
        }

        Throwable cause = e.getCause();
        int safetyCounter = 0;
        while ( cause != null && safetyCounter < 10 )
        {
            safetyCounter++;
            errorMsg.append( ", cause:" ).append( cause.getClass().getName() );
            if ( cause.getMessage() != null )
            {
                errorMsg.append( ": " ).append( cause.getMessage() );
            }
            cause = cause.getCause();
        }

        return errorMsg.toString();
    }

    public static <E extends Enum<E>> boolean enumArrayContainsValue( final E[] enumArray, final E enumValue )
    {
        return !( enumArray == null || enumArray.length == 0 ) && Arrays.asList( enumArray ).contains( enumValue );
    }

    public static void unhandledSwitchStatement( final Object switchParameter )
    {
        final String className = switchParameter == null
                ? "unknown - see stack trace"
                : switchParameter.getClass().getName();

        final String paramValue = switchParameter == null
                ? "unknown"
                : switchParameter.toString();

        final String errorMsg = "unhandled switch statement on parameter class=" + className + ", value=" + paramValue;
        final UnsupportedOperationException exception = new UnsupportedOperationException( errorMsg );
        LOGGER.warn( () -> errorMsg, exception );
        throw exception;
    }

    public static long copy( final InputStream input, final OutputStream output )
            throws IOException
    {
        final int bufferSize = 4 * 1024;
        final byte[] buffer = new byte[bufferSize];
        return IOUtils.copyLarge( input, output, 0, -1, buffer );
    }

    public static String copyToString( final InputStream input )
            throws IOException
    {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        JavaHelper.copy( input, byteArrayOutputStream );
        return byteArrayOutputStream.toString( PwmConstants.DEFAULT_CHARSET );
    }

    public static ImmutableByteArray copyToBytes( final InputStream inputStream, final int maxLength )
            throws IOException
    {
        final byte[] bytes = IOUtils.toByteArray( inputStream, maxLength );
        return ImmutableByteArray.of( bytes );
    }

    public static void copy( final String input, final OutputStream output )
            throws IOException
    {
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream( input.getBytes( PwmConstants.DEFAULT_CHARSET ) );
        JavaHelper.copy( byteArrayInputStream, output );
    }

    public static long copyWhilePredicate(
            final InputStream input,
            final OutputStream output,
            final Predicate<Long> predicate
    )
            throws IOException
    {
        return copyWhilePredicate( input, output, 4 * 1024, predicate, null );
    }

    public static long copyWhilePredicate(
            final InputStream input,
            final OutputStream output,
            final int bufferSize,
            final Predicate<Long> predicate,
            final ConditionalTaskExecutor conditionalTaskExecutor
    )
            throws IOException
    {
        final byte[] buffer = new byte[bufferSize];
        int bytesCopied;
        long totalCopied = 0;
        do
        {
            bytesCopied = input.read( buffer );
            if ( bytesCopied > 0 )
            {
                output.write( buffer, 0, bytesCopied );
                totalCopied += bytesCopied;
            }
            if ( conditionalTaskExecutor != null )
            {
                conditionalTaskExecutor.conditionallyExecuteTask();
            }
            if ( !predicate.test( totalCopied ) )
            {
                return totalCopied;
            }
        }
        while ( bytesCopied > 0 );
        return totalCopied;
    }

    public static String toIsoDate( final Instant instant )
    {
        return instant == null ? "" : instant.truncatedTo( ChronoUnit.SECONDS ).toString();
    }

    public static String toIsoDate( final Date date )
    {
        if ( date == null )
        {
            return "";
        }

        final PwmDateFormat dateFormat = PwmDateFormat.newPwmDateFormat(
                PwmConstants.DEFAULT_DATETIME_FORMAT_STR,
                PwmConstants.DEFAULT_LOCALE,
                PwmConstants.DEFAULT_TIMEZONE
        );

        return dateFormat.format( date.toInstant() );
    }

    public static Instant parseIsoToInstant( final String input )
    {
        return Instant.parse( input );
    }

    public static void closeAndWaitExecutor( final ExecutorService executor, final TimeDuration timeDuration )
    {
        if ( executor == null )
        {
            return;
        }

        executor.shutdown();
        try
        {
            executor.awaitTermination( timeDuration.asMillis(), TimeUnit.MILLISECONDS );
        }
        catch ( final InterruptedException e )
        {
            LOGGER.warn( () -> "unexpected error shutting down executor service " + executor.getClass().toString() + " error: " + e.getMessage() );
        }
    }

    public static Collection<Method> getAllMethodsForClass( final Class clazz )
    {
        final LinkedHashSet<Method> methods = new LinkedHashSet<>( Arrays.asList( clazz.getDeclaredMethods() ) );

        final Class superClass = clazz.getSuperclass();
        if ( superClass != null )
        {
            methods.addAll( getAllMethodsForClass( superClass ) );
        }

        return Collections.unmodifiableSet( methods );
    }

    public static CSVPrinter makeCsvPrinter( final OutputStream outputStream )
            throws IOException
    {
        return new CSVPrinter( new OutputStreamWriter( outputStream, PwmConstants.DEFAULT_CHARSET ), PwmConstants.DEFAULT_CSV_FORMAT );
    }

    /**
     * Copy of {@link ThreadInfo#toString()} but with the MAX_FRAMES changed from 8 to 256.
     * @param threadInfo thread information
     * @return a stacktrace string with newline formatting
     */
    public static String threadInfoToString( final ThreadInfo threadInfo )
    {
        final int maxFrames = 256;
        final StringBuilder sb = new StringBuilder( "\"" + threadInfo.getThreadName() + "\""
                + " Id=" + threadInfo.getThreadId() + " "
                + threadInfo.getThreadState() );
        if ( threadInfo.getLockName() != null )
        {
            sb.append( " on " ).append( threadInfo.getLockName() );
        }
        if ( threadInfo.getLockOwnerName() != null )
        {
            sb.append( " owned by \"" ).append( threadInfo.getLockOwnerName() ).append( "\" Id=" ).append( threadInfo.getLockOwnerId() );
        }
        if ( threadInfo.isSuspended() )
        {
            sb.append( " (suspended)" );
        }
        if ( threadInfo.isInNative() )
        {
            sb.append( " (in native)" );
        }
        sb.append( '\n' );

        int counter = 0;

        for (; counter < threadInfo.getStackTrace().length && counter < maxFrames; counter++ )
        {
            final StackTraceElement ste = threadInfo.getStackTrace()[ counter ];
            sb.append( "\tat " ).append( ste.toString() );
            sb.append( '\n' );
            if ( counter == 0 && threadInfo.getLockInfo() != null )
            {
                final Thread.State ts = threadInfo.getThreadState();
                switch ( ts )
                {
                    case BLOCKED:
                        sb.append( "\t-  blocked on " ).append( threadInfo.getLockInfo() );
                        sb.append( '\n' );
                        break;
                    case WAITING:
                        sb.append( "\t-  waiting on " ).append( threadInfo.getLockInfo() );
                        sb.append( '\n' );
                        break;
                    case TIMED_WAITING:
                        sb.append( "\t-  waiting on " ).append( threadInfo.getLockInfo() );
                        sb.append( '\n' );
                        break;
                    default:
                }
            }

            for ( final MonitorInfo mi : threadInfo.getLockedMonitors() )
            {
                if ( mi.getLockedStackDepth() == counter )
                {
                    sb.append( "\t-  locked " ).append( mi );
                    sb.append( '\n' );
                }
            }
        }
        if ( counter < threadInfo.getStackTrace().length )
        {
            sb.append( "\t..." );
            sb.append( '\n' );
        }

        final LockInfo[] locks = threadInfo.getLockedSynchronizers();
        if ( locks.length > 0 )
        {
            sb.append( "\n\tNumber of locked synchronizers = " ).append( locks.length );
            sb.append( '\n' );
            for ( final LockInfo li : locks )
            {
                sb.append( "\t- " ).append( li );
                sb.append( '\n' );
            }
        }
        sb.append( '\n' );
        return sb.toString();
    }

    public static int rangeCheck( final int min, final int max, final int value )
    {
        return ( int ) rangeCheck( ( long ) min, ( long ) max, ( long ) value );
    }

    public static long rangeCheck( final long min, final long max, final long value )
    {
        if ( min > max )
        {
            throw new IllegalArgumentException( "min range is greater than max range" );
        }
        if ( max < min )
        {
            throw new IllegalArgumentException( "max range is less than min range" );
        }
        long returnValue = value;
        if ( value < min )
        {
            returnValue = min;
        }
        if ( value > max )
        {
            returnValue = max;
        }
        return returnValue;
    }

    public static <T> Optional<T> extractNestedExceptionType( final Exception inputException, final Class<T> exceptionType )
    {
        if ( inputException == null )
        {
            return Optional.empty();
        }

        if ( inputException.getClass().isAssignableFrom( exceptionType ) )
        {
            return Optional.of( ( T ) inputException );
        }

        Throwable nextException = inputException.getCause();
        while ( nextException != null )
        {
            if ( nextException.getClass().isAssignableFrom( exceptionType ) )
            {
                return Optional.of( ( T ) inputException );
            }

            nextException = nextException.getCause();
        }

        return Optional.empty();
    }

    /**
     * Very naive implementation to get a rough order estimate of object memory size, used for debug
     * purposes only.
     *
     * @param object object to be analyzed
     * @return size of object (very rough estimate)
     */
    public static long sizeof( final Serializable object )
    {
        try ( ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream() )
        {
            final ObjectOutputStream out = new ObjectOutputStream( byteArrayOutputStream );
            out.writeObject( object );
            out.flush();
            return byteArrayOutputStream.toByteArray().length;
        }
        catch ( final IOException e )
        {
            LOGGER.debug( () -> "exception while estimating session size: " + e.getMessage() );
            return 0;
        }
    }

    public static Map<String, String> propertiesToStringMap( final Properties properties )
    {
        Objects.requireNonNull( properties );
        final Map<String, String> returnMap = new LinkedHashMap<>( properties.size() );
        properties.forEach( ( key, value ) -> returnMap.put( ( String ) key, ( String ) value ) );
        return returnMap;
    }

    public static LongAccumulator newAbsLongAccumulator()
    {
        return new LongAccumulator( ( left, right ) ->
        {
            final long newValue = left + right;
            return newValue < 0 ? 0 : newValue;
        }, 0L );
    }

    public static Properties newSortedProperties()
    {
        return new org.apache.commons.collections4.properties.SortedProperties();
    }

    public static int silentParseInt( final String input, final int defaultValue )
    {
        try
        {
            return Integer.parseInt( input );
        }
        catch ( final NumberFormatException e )
        {
            return defaultValue;
        }
    }

    public static long silentParseLong( final String input, final long defaultValue )
    {
        try
        {
            return Long.parseLong( input );
        }
        catch ( final NumberFormatException e )
        {
            return defaultValue;
        }
    }

    public static boolean doubleContainsLongValue( final Double input )
    {
        return input.equals( Math.floor( input ) )
                && !Double.isInfinite( input )
                && !Double.isNaN( input )
                && input <= Long.MAX_VALUE
                && input >= Long.MIN_VALUE;
    }

    public static byte[] longToBytes( final long input )
    {
        return ByteBuffer.allocate( 8 ).putLong( input ).array();
    }

    public static String requireNonEmpty( final String input )
    {
        return requireNonEmpty( input, "non-empty string value required" );
    }

    public static String requireNonEmpty( final String input, final String message )
    {
        if ( StringUtil.isEmpty( input ) )
        {
            throw new NullPointerException( message );
        }
        return input;
    }

    public static byte[] gunzip( final byte[] bytes )
            throws IOException
    {
        try (  GZIPInputStream inputGzipStream = new GZIPInputStream( new ByteArrayInputStream( bytes ) ) )
        {
            return inputGzipStream.readAllBytes();
        }
    }

    public static byte[] gzip( final byte[] bytes )
            throws IOException
    {
        try ( ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             GZIPOutputStream gzipOutputStream = new GZIPOutputStream( byteArrayOutputStream ) )
        {
            gzipOutputStream.write( bytes );
            gzipOutputStream.close();
            return byteArrayOutputStream.toByteArray();
        }
    }

    public static byte[] addByteArrays( final byte[] a, final byte[] b )
    {
        final byte[] newByteArray = new byte[ a.length + b.length ];
        System.arraycopy( a, 0, newByteArray, 0, a.length );
        System.arraycopy( b, 0, newByteArray, a.length, b.length );
        return newByteArray;
    }
}
