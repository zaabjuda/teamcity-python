package jetbrains.buildServer.python.common;

import org.jetbrains.annotations.NotNull;


/**
 * Version number of the python.
 * @author Leonid Bushuev from JetBrains
 */
public final class PythonVersion
    implements Comparable<PythonVersion>
{
    /**
     * Major version part.
     */
    public final int major;

    /**
     * Minor version part.
     */
    public final int minor;

    /**
     * String representation of the version number.
     */
    @NotNull
    public final String string;


    public PythonVersion(int major, int minor, @NotNull String string)
    {
        this.major = major;
        this.minor = minor;
        this.string = string;
    }


    @Override
    public String toString()
    {
        return major+"."+minor+" ["+string+"]";
    }


    @Override
    public int compareTo(@NotNull PythonVersion that)
    {
        int z = this.major - that.major;
        if (z == 0)
            z = this.minor - that.minor;
        if (z == 0)
            z = this.string.compareTo(that.string);
        return z;
    }


    @Override
    public boolean equals(Object that)
    {
        if (this == that) return true;
        if (that == null) return false;

        return (that instanceof PythonVersion && this.equals((PythonVersion)that));
    }

    public boolean equals(PythonVersion that)
    {
        return this.major == that.major
            && this.minor == that.minor
            && this.string.equals(that.string);
    }


    @Override
    public int hashCode()
    {
        return (major << 16) | minor;
    }


}
