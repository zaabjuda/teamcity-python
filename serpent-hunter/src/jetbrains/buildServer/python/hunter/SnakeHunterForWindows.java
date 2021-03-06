package jetbrains.buildServer.python.hunter;

import jetbrains.buildServer.util.Bitness;
import jetbrains.buildServer.utils.WinRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileFilter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static jetbrains.buildServer.utils.YASU.*;


/**
 * Python detector for Windows.
 * @author Leonid Bushuev from JetBrains
 */
class SnakeHunterForWindows extends SnakeHunter
{

    private final WinRegistry winReg = new WinRegistry();



    //// CLASSIC PYTHONS HUNTING \\\\\


    private static final String ourClassicPythonRegPath = "HKLM\\SOFTWARE\\Python";

    private static final Pattern ourClassicPythonRegKeyPattern =
            Pattern.compile("HK[A-Z_]*\\\\SOFTWARE\\\\Python\\\\PythonCore\\\\.*\\\\InstallPath", Pattern.CASE_INSENSITIVE);


    @Override
    protected void collectDirsToLookForClassicPython(Set<File> dirsToLook)
    {
        List<File> dirsFromRegistry = peekClassicPythonsFromWinRegistry();
        dirsToLook.addAll(dirsFromRegistry);
        dirsToLook.addAll(runPaths);
    }


    @Override
    protected Pattern getClassicPythonExeFileNamePattern()
    {
        return Pattern.compile("python.exe", Pattern.CASE_INSENSITIVE);
    }


    private List<File> peekClassicPythonsFromWinRegistry()
    {
        return peekPythonsFromWinRegistry(ourClassicPythonRegPath, ourClassicPythonRegKeyPattern);
    }



    //// IRON PYTHONS HUNTING \\\\\


    private static final String ourIronPythonRegPath = "HKLM\\SOFTWARE\\IronPython";

    private static final Pattern ourIronPythonRegKeyPattern =
            Pattern.compile("HK[A-Z_]*\\\\SOFTWARE\\\\IronPython\\\\.*\\\\InstallPath", Pattern.CASE_INSENSITIVE);


    @Override
    protected void collectDirsToLookForIronPython(Set<File> dirsToLook)
    {
        List<File> dirsFromRegistry = peekIronPythonsFromWinRegistry();
        dirsToLook.addAll(dirsFromRegistry);
        dirsToLook.addAll(runPaths);

        lookForIronPythonLikeDirs(System.getenv("ProgramFiles"), dirsToLook);
        lookForIronPythonLikeDirs(System.getenv("ProgramW6432"), dirsToLook);
        lookForIronPythonLikeDirs(System.getenv("ProgramFiles(x86)"), dirsToLook);
        lookForIronPythonLikeDirs("C:\\Program Files", dirsToLook);
        lookForIronPythonLikeDirs("C:\\App", dirsToLook);
        lookForIronPythonLikeDirs("D:\\App", dirsToLook);
        lookForIronPythonLikeDirs("E:\\App", dirsToLook);

        for (File dirFromRegistry : dirsFromRegistry)
            lookForSiblingDirs(dirFromRegistry, dirsToLook);
    }


    private void lookForIronPythonLikeDirs(final @Nullable String outerPath, @NotNull Collection<File> dirs)
    {
        String path = trimAndNull(outerPath);
        if (path == null)
            return;

        File outer = new File(path);
        if (!outer.exists() || !outer.isDirectory())
            return;

        FileFilter filter = new FileFilter()
        {
            @Override
            public boolean accept(File pathname)
            {
                return pathname.isDirectory()
                    && pathname.getName().toLowerCase().startsWith("ironpython");
            }
        };

        Collections.addAll(dirs, outer.listFiles(filter));
    }

    private void lookForSiblingDirs(File dir, Set<File> dirsToLook)
    {
        File parent = dir.getParentFile();
        if (!parent.isDirectory()) return;

        FileFilter filter = new FileFilter()
        {
            @Override
            public boolean accept(File pathname)
            {
                return pathname.isDirectory();
            }
        };

        Collections.addAll(dirsToLook, parent.listFiles(filter));
    }



    @Override
    protected Pattern getIronPythonExeFileNamePattern()
    {
        return Pattern.compile("ipy(32|64)?\\.exe", Pattern.CASE_INSENSITIVE);
    }


    private List<File> peekIronPythonsFromWinRegistry()
    {
        return peekPythonsFromWinRegistry(ourIronPythonRegPath, ourIronPythonRegKeyPattern);
    }



    //// JYTHONS HUNTING \\\\



    //// COMMON ROUTINES \\\\


    private List<File> peekPythonsFromWinRegistry(final String regPath, final Pattern regKeyPattern)
    {
        final List<File> dirsWithPythons = new ArrayList<File>(4);

        for (Bitness bitness: Arrays.asList(null, Bitness.BIT64, Bitness.BIT32))
        {
            WinRegistry.DumpConsumer consumer =
                    new WinRegistry.DumpConsumer()
                    {
                        boolean keyToProcess = false;

                        @Override
                        public void handleKey(@NotNull String keyName)
                        {
                            Matcher m = regKeyPattern.matcher(keyName);
                            keyToProcess = m.matches();
                        }

                        @Override
                        public void handleValue(@NotNull String entryName, @NotNull String entryValue)
                        {
                            if (keyToProcess && entryName.equals(""))
                            {
                                File file = new File(entryValue);
                                dirsWithPythons.add(file);
                            }
                        }
                    };

            try
            {
                winReg.dump(regPath, bitness, consumer);
            }
            catch (WinRegistry.Error wre)
            {
                String bitnessStr = bitness == null ? "default" : Byte.toString(bitness.value);
                System.err.println("WinRegistry ("+ ourClassicPythonRegPath +"), bitness: " + bitnessStr + ":\n" + wre.getMessage());
            }
        }

        return dirsWithPythons;
    }




}
