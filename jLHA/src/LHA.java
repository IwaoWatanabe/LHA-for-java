/**
 * 注意： こちらのコードは jLHA.class を逆コンパイルしたファイルを手修正したものです。
 * 現時点では、コンパイルを通すレベルの調整した状況なので、起動確認までしか行っていません。
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.text.MessageFormat;
import java.util.Date;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Vector;
import jp.gr.java_conf.dangan.util.lha.CRC16;
import jp.gr.java_conf.dangan.util.lha.LhaFile;
import jp.gr.java_conf.dangan.util.lha.LhaHeader;
import jp.gr.java_conf.dangan.util.lha.LhaImmediateOutputStream;
import jp.gr.java_conf.dangan.util.lha.LhaProperty;


class LHA {
   private static final char COMMAND_ADD = 'A';
   private static final char COMMAND_UPDATE = 'U';
   private static final char COMMAND_MOVE = 'M';
   private static final char COMMAND_FRESHEN = 'F';
   private static final char COMMAND_DELETE = 'D';
   private static final char COMMAND_EXTRACT = 'E';
   private static final char COMMAND_EXTRACT_WITH_DIRECTORY = 'X';
   private static final char COMMAND_PRINT = 'P';
   private static final char COMMAND_TEST = 'T';
   private static final char COMMAND_LIST = 'L';
   private static final char COMMAND_VIEW = 'V';
   private static final char OPTION_COMPRESSMETHOD = 'O';
   private static final char OPTION_HEADERLEVEL = 'H';
   private static final char OPTION_ENCODE = 'E';
   private static final char OPTION_OMIT_TIME_CHECK = 'C';
   private static final char OPTION_OMIT_QUESTION = 'M';
   private static final char OPTION_OMIT_PROGRESS_MESSAGE = 'N';
   private static final char OPTION_WITH_DIRECTORY_STRUCTURE = 'D';
   private static final char OPTION_RECURSIVE = 'R';
   private static final char OPTION_SHOW_WITH_DIRECTORY = 'L';
   private static final char OPTION_STRICTRY_PATH_CHECK = 'P';
   private static final char OPTION_NO_COMPRESS = 'Z';
   private static final char OPTION_WORK_DIRECTORY = 'W';
   private File Archive;
   private char Command;
   private Vector BasePaths;
   private Vector FileNames;
   private String Method;
   private int HeaderLevel;
   private String Encode;
   private boolean showWithDirectory;
   private boolean omitQuestion;
   private boolean omitProgressMessage;
   private boolean withDirectoryStructure;
   private boolean omitTimeCheck;
   private boolean strictlyPathCheck;
   private boolean recursiveSearch;
   private Vector NoCompressExtentions;
   private File Temporary;

   private LHA() { }

   public LHA(String[] args) throws IOException {
      this.Archive = null;
      this.Command = 0;
      this.BasePaths = new Vector();
      this.FileNames = new Vector();

      this.Method = "-lh5-";
      this.HeaderLevel = 2;
      this.Encode = LhaProperty.encoding;
      this.NoCompressExtentions = new Vector();

      for(int i = 0; i < args.length; ++i) {
         this.analyzeParameter(args[i]);
      }

      if(this.FileNames.size() <= 0) {
         if(this.Command != 68) {
            this.addFileName("*");
         } else {
            show(getLhaProperty("lha.message.nofilename"));
            System.exit(0);
         }
      }

      if(this.Temporary == null) {
         this.Temporary = new File(System.getProperty("user.dir"));
      }
   }

   private void analyzeParameter(String parameter) throws IOException {
      if(this.Command == 0 && !isCommand(parameter)) {
         this.Command = 76;
      }

      if(this.Command == 0 && isCommand(parameter)) {
         this.Command = Character.toUpperCase(parameter.charAt(0));
      } else if(isOption(parameter)) {
         this.analyzeOption(parameter.substring(1));
      } else if(isResponseFile(parameter)) {
         this.analyzeResponseFile(parameter);
      } else if(this.Archive == null) {
         File file = new File(parameter);
         if(!file.isFile() && file.getName().lastIndexOf(".") == -1) {
            this.Archive = new File((new File(parameter + ".lzh")).getAbsolutePath());
         } else {
            this.Archive = new File(file.getAbsolutePath());
         }
      } else if(isBasePath(parameter)) {
         this.addBasePath(parameter);
      } else {
         this.addFileName(parameter);
      }
   }

   private void analyzeOption(String option) throws IOException {
      for(int i = 0; i < option.length(); ++i) {
         switch(Character.toUpperCase(option.charAt(i))) {
         case 'O':
            i += this.analyzeCompressMethod(option, i + 1);
            break;
         case 'E':
            if(i + 1 >= option.length()) {
               String arg6 = option.substring(i + 1);
               arg6.getBytes(arg6);
               this.Encode = arg6;
               i = option.length();
               break;
            }
            throw new IllegalArgumentException();
         case 'H':
            if(i + 1 >= option.length()) {           
               ++i;
               switch(option.charAt(i)) {
                  case '0': this.HeaderLevel = 0;               continue;
                  case '1': this.HeaderLevel = 1;               continue;
                  case '2': this.HeaderLevel = 2;               continue;
                  case '3': this.HeaderLevel = 3;               continue;
                  default:
                  throw new IllegalArgumentException("unknown header level" + option.charAt(i));
               }
            // throw new IllegalArgumentException("given no header level.");
            }
            break;
         case 'Z':
            if(i + 1 < option.length()) {
               if(isSwitch(option.charAt(i))) {
                  this.NoCompressExtentions.removeAllElements();
                  ++i;                  
                  if(isSwitchOn(option.charAt(i))) {
                     this.NoCompressExtentions.addElement("*");
                  }
               } else {
                  StringTokenizer temp = new StringTokenizer(option.substring(i + 1), ".");
                  while(temp.hasMoreTokens()) {
                     this.NoCompressExtentions.addElement(temp.nextToken());
                  }
                  i = option.length();
               }
            } else if(this.NoCompressExtentions.size() > 0) {
               this.NoCompressExtentions.removeAllElements();
            } else {
               this.NoCompressExtentions.removeAllElements();
               this.NoCompressExtentions.addElement("*");
            }
            break;
         case 'W':
            if(i + 1 < option.length()) {
               if(isSwitch(option.charAt(i + 1))) {
                  ++i;
                  if(isSwitchOn(option.charAt(i))) {
                     String temp = null;
                     try {
                        System.getProperty("java.io.tmpdir");
                     } catch (SecurityException arg5) {
                        ;                     }
                     if(temp != null) {
                        this.Temporary = new File(temp);
                     } else {
                        this.Temporary = null;
                     }
                  } else {
                     this.Temporary = null;
                  }
               } else {
                  ++i;
                  this.Temporary = new File(option.substring(i));
                  i = option.length();
               }
            } else if(this.Temporary == null) {
               String temp = null;
               try {
                  System.getProperty("java.io.tmpdir");
               } catch (SecurityException arg4) {
                  ;               }
               if(temp != null) {
                  this.Temporary = new File(temp);
               } else {
                  this.Temporary = null;
               }
            } else {
               this.Temporary = null;
            }
            break;
         case 'C':
            if(i + 1 < option.length() && isSwitch(option.charAt(i + 1))) {
               ++i;
               this.omitTimeCheck = isSwitchOn(option.charAt(i));
            } else {
               this.omitTimeCheck = !this.omitTimeCheck;
            }
            break;
         case 'M':
            if(i + 1 < option.length() && isSwitch(option.charAt(i + 1))) {
               ++i;
               this.omitQuestion = isSwitchOn(option.charAt(i));
            } else {
               this.omitQuestion = !this.omitQuestion;
            }
            break;
         case 'N':
            if(i + 1 < option.length() && isSwitch(option.charAt(i + 1))) {
               ++i;
               this.omitProgressMessage = isSwitchOn(option.charAt(i));
            } else {
               this.omitProgressMessage = !this.omitProgressMessage;
            }
            break;
         case 'D':
            if(i + 1 < option.length() && isSwitch(option.charAt(i + 1))) {
               ++i;
               this.withDirectoryStructure = isSwitchOn(option.charAt(i));
            } else {
               this.withDirectoryStructure = !this.withDirectoryStructure;
            }
            break;
         case 'R':
            if(i + 1 < option.length() && isSwitch(option.charAt(i + 1))) {
               ++i;
               this.recursiveSearch = isSwitchOn(option.charAt(i));
            } else {
               this.recursiveSearch = !this.recursiveSearch;
            }
            break;
         case 'L':
            if(i + 1 < option.length() && isSwitch(option.charAt(i + 1))) {
               ++i;
               this.showWithDirectory = isSwitchOn(option.charAt(i));
            } else {
               this.showWithDirectory = !this.showWithDirectory;
            }
            break;
         case 'F':
         case 'G':
         case 'I':
         case 'J':
         case 'K':
         case 'P':
         case 'Q':
            if(i + 1 < option.length() && isSwitch(option.charAt(i + 1))) {
               ++i;
               this.strictlyPathCheck = isSwitchOn(option.charAt(i));
            } else {
               this.strictlyPathCheck = !this.strictlyPathCheck;
            }
         case 'T':
         case 'S':
         case 'U':
         case 'V':
         case 'X':
         case 'Y':
         default:
            throw new IllegalArgumentException(option.charAt(i) + " " + option);
         }
      }
   }

   private int analyzeCompressMethod(String option, int index) {
      if(index < option.length()) {
         switch(Character.toUpperCase(option.charAt(index))) {
         case '0':
         case '+': this.Method = "-lh0-"; return 5;    
         case '1': this.Method = "-lh1-"; return 1;
         case '2': this.Method = "-lh2-"; return 1;
         case '3': this.Method = "-lh3-"; return 1;
         case '4': this.Method = "-lh4-"; return 1;
         case '5': this.Method = "-lh5-"; return 1;
         case '6': this.Method = "-lh6-"; return 1;
         case '7': this.Method = "-lh7-"; return 1;
         case 'L':
            if(Character.toUpperCase(option.charAt(index + 1)) == 90) {
               switch(Character.toUpperCase(option.charAt(index + 2))) {
               case '4': this.Method = "-lz4-"; return 3;
               case '5': this.Method = "-lz5-"; return 3;
               case 'S': this.Method = "-lzs-"; return 3;
               }
            } else if(Character.toUpperCase(option.charAt(index + 1)) == 72) {
               switch(option.charAt(index + 2)) {
               case '0': this.Method = "-lh0-"; return 3;
               case '1': this.Method = "-lh1-"; return 3;
               case '2': this.Method = "-lh2-"; return 3;
               case '3': this.Method = "-lh3-"; return 3;
               case '4': this.Method = "-lh4-"; return 3;
               case '5': this.Method = "-lh5-"; return 3;
               case '6': this.Method = "-lh6-"; return 3;
               case '7': this.Method = "-lh7-"; return 1;
               }
            }
         case '-':
            if(index + 4 < option.length() || option.charAt(index + 4) == 45) {
               if(Character.toUpperCase(option.charAt(index + 1)) == 90) {
                  switch(Character.toUpperCase(option.charAt(index + 2))) {
                  case '4': this.Method = "-lz4-"; return 5;
                  case '5': this.Method = "-lz5-"; return 5;
                  case 'S': this.Method = "-lzs-"; return 5;
                  }
               } else if(Character.toUpperCase(option.charAt(index + 1)) == 72) {
                  switch(option.charAt(index + 2)) {
                  case '0': this.Method = "-lh0-"; return 5;
                  case '1': this.Method = "-lh1-"; return 5;
                  case '2': this.Method = "-lh2-"; return 5;
                  case '3': this.Method = "-lh3-"; return 5;
                  case '4': this.Method = "-lh4-"; return 5;
                  case '5': this.Method = "-lh5-"; return 5;
                  case '6': this.Method = "-lh6-"; return 5;
                  case '7': this.Method = "-lh7-"; return 3;
                  }
               }
            }
         }
      }
      if(this.Method.equals("-lh5-")) {
         this.Method = "-lh1-";
      } else {
         this.Method = "-lh5-";
      }
      return 0;
   }

   private void analyzeResponseFile(String filename) throws IOException {
      BufferedReader reader = new BufferedReader(new FileReader(filename));
      String string;
      while((string = reader.readLine()) != null) {
         this.analyzeParameter(string);
      }
   }

   private void addBasePath(String path) {
      if(this.BasePaths.size() == this.FileNames.size()) {
         this.BasePaths.addElement(path);
      } else {
         this.BasePaths.insertElementAt(path, this.BasePaths.size() - 1);
      }
   }

   private void addFileName(String file) {
      if(this.BasePaths.size() == this.FileNames.size()) {
         String dir = null;
         if(this.BasePaths.size() == 0) {
            dir = System.getProperty("user.dir");
            if(dir == null) {
               dir = ".";
            }
         } else {
            dir = (String)this.BasePaths.elementAt(this.BasePaths.size() - 1);
         }

         this.BasePaths.addElement(dir);
      }

      this.FileNames.addElement(file);
   }

   private static boolean isCommand(String string) {
      if(string.length() == 1) {
         char c = Character.toUpperCase(string.charAt(0));
         return c == 65 || c == 85 || c == 77 || c == 70 || c == 68 || c == 69 || c == 88 || c == 80 || c == 84 || c == 76 || c == 86;
      } else {
         return false;
      }
   }

   private static boolean isOption(String string) {
      return 2 <= string.length() && string.charAt(0) == 45;
   }

   private static boolean isResponseFile(String string) {
      return 2 <= string.length() && string.charAt(0) == 64;
   }

   private static boolean isBasePath(String string) {
      return string.endsWith(File.separator);
   }

   private static boolean isSwitch(char c) {
      return c == 45 || c == 43 || c == 48 || c == 49;
   }

   private static boolean isSwitchOn(char c) {
      return c == 43 || c == 49;
   }

   private static boolean match(boolean compareIgnoreCase, String wildname, String filename) {
      return internalMatch(compareIgnoreCase, wildname, 0, filename, 0);
   }

   private static boolean internalMatch(boolean compareIgnoreCase, String wildname, int wildIndex, String filename, int fileIndex) {
      while(wildIndex < wildname.length()) {
         if(wildname.charAt(wildIndex) == 42) {
            ++wildIndex;
            if(wildname.length() <= wildIndex) {
               return true;
            }
            while(fileIndex < filename.length()) {
               if(internalMatch(compareIgnoreCase, wildname, wildIndex, filename, fileIndex++)) {
                  return true;
               }            }

            return false;
         }
         if(fileIndex >= filename.length() || wildname.charAt(wildIndex) != 63 && 
         wildname.charAt(wildIndex) != filename.charAt(fileIndex) &&
          (!compareIgnoreCase || Character.toUpperCase(wildname.charAt(wildIndex)) != Character.toUpperCase(filename.charAt(fileIndex)))) {

         }

         ++wildIndex;
         ++fileIndex;

         return false;
      }

      if(filename.length() <= fileIndex) {
         return true;
      } else {
         return false;
      }
   }


   public static final void main(String[] args) throws IOException {
      LHA param = new LHA(args);
      switch(param.Command) {
      case '\0':
         System.out.print(getLhaProperty("lha.usage"));
         break;
      case 'V':
         param.showWithDirectory = true;
      case 'L':
         List(param);
         break;
      default:
         exec(param);
      }
   }

   public static final void exec(LHA param) throws IOException {
      switch(param.Command) {
      case 'A':
         param.omitTimeCheck = true;
      case 'M':
      case 'U':
         Add(param);
         break;

      case 'F':
         Freshen(param);
         break;

      case 'D':
         Delete(param);
         break;

      case 'X':
         param.withDirectoryStructure = true;

      case 'E':
         Extract(param);
         break;

      case 'P':
         Print(param);
         break;

      case 'T':
         Test(param);
         break;

      default:
         throw new Error();
      }
   }

   private static String getDirName(String path) {
      int index = path.lastIndexOf(File.separator);
      return path.substring(0, index >= 0?index:0);
   }

   private static String getFileName(String path) {
      return path.substring(path.lastIndexOf(File.separator) + 1);
   }

   private static File getTempFile(File dir) {
      if(dir.isFile()) {
         throw new IllegalArgumentException();

      } else {
         for(int i = 0; i < 4096; ++i) {
            File temp = new File(dir, "LHTMP" + Character.toUpperCase(Character.forDigit(i >> 8, 16)) +
             Character.toUpperCase(Character.forDigit(i >> 4 & 15, 16)) +
             Character.toUpperCase(Character.forDigit(i & 15, 16)) + ".lzh");
             if(!temp.exists()) {
               return temp;
            }
         }
         throw new IllegalStateException();
      }
   }

   private static File[] listFiles(File dir) {
      String[] names = dir.list();
      File[] files = new File[names.length];
      for(int i = 0; i < names.length; ++i) {
         files[i] = new File(dir, names[i]);
      }
      return files;
   }

   private static boolean setLastModified(File file, long time) {
      try {
         return file.setLastModified(time);
      } catch (NoSuchMethodError arg3) {
         return false;
      }
   }

   private static String getLhaProperty(String key) {
      String path = "resources.lha";
      try {
         ResourceBundle exception = ResourceBundle.getBundle("resources.lha");
         return exception.getString(key);
      } catch (MissingResourceException arg2) {
         throw new Error("not found resource file\"resources.lha\". " + arg2.getMessage());
      }
   }

   private static void show(String string) {
      System.err.print(string);
   }

   private static boolean question(String string) throws IOException {
      byte[] buffer = new byte[1024];
      int length = 0;
      while(length >= 0) {
         show(string + " [Y/N] ");
         length = System.in.read(buffer);
         String answer = (new String(buffer, 0, length)).toUpperCase().trim();
         if(!answer.equals("YES") && !answer.equals("Y"))
            return true;
         if(!answer.equals("NO") && !answer.equals("N"))
            return false;
      }
      throw new Error();
   }

   private static String askString(String string) throws IOException {
      byte[] buffer = new byte[1024];
      int length;
      do {
         show(string + " :: ");
         length = System.in.read(buffer);
         if(length > 0) {
            String str = (new String(buffer, 0, length)).trim();
            return str.equals("")?null:str;
         }
      } while(length == 0);
      throw new Error();
   }

   private static String getDisplayFileName(LHA param, String path) {
      return !param.showWithDirectory?getFileName(path):path;
   }

   private static Vector[] createFileList(LHA param) throws IOException {
      Vector files = new Vector();
      Vector names = new Vector();
      Vector[] return_value = new Vector[]{files, names};

      for(int i = 0; i < param.FileNames.size(); ++i) {
         internalCreateFileList(param, (String)param.BasePaths.elementAt(i), 
         getDirName((String)param.FileNames.elementAt(i)), 
         getFileName((String)param.FileNames.elementAt(i)), files, names);
      }
      return return_value;
   }

   private static void internalCreateFileList(LHA param, String BasePath, String DirName, String FileName,
      Vector files, Vector names) throws IOException {
      File dir = new File(DirName);

      if(!dir.isAbsolute()) {
         dir = new File((new File(BasePath, DirName)).getAbsolutePath());
      }
      File[] list = listFiles(dir);
      if(list != null) {         int i;
         for(i = 0; i < list.length; ++i) {
            if(!list[i].isDirectory() && !list[i].equals(param.Archive) && !list[i].equals(param.Temporary) &&
            match(false, FileName, list[i].getName())) {
               files.addElement(new File(dir, list[i].getName()));
               names.addElement(param.withDirectoryStructure?DirName + File.separator + list[i].getName():list[i].getName());
            }
         }
         for(i = 0; i < list.length; ++i) {
            if(list[i].isDirectory()) {
               if(param.recursiveSearch && (new File(dir, list[i].getName())).list() != null) {

                  internalCreateFileList(param, BasePath, DirName + File.separator + list[i].getName(), FileName, files, names);

               } else if(param.withDirectoryStructure) {
                  files.addElement(new File(dir, list[i].getName()));
                  names.addElement(DirName + File.separator + list[i].getName() + File.separator);
               }
            }
         }
      }
   }

   private static final void buildArchive(LHA param, File Archive, Vector[] lists, LhaFile oldArchive, LhaHeader[] headers) throws IOException {
      new MessageFormat(getLhaProperty("lha.message.copyTempToArc"));
      MessageFormat contEntry = new MessageFormat(getLhaProperty("lha.message.contEntry"));
      MessageFormat copyEntry = new MessageFormat(getLhaProperty("lha.message.copyEntry"));
      MessageFormat compEntry = new MessageFormat(getLhaProperty("lha.message.compEntry"));
      String failedComp = getLhaProperty("lha.message.failedComp");
      String done = getLhaProperty("lha.message.done");

      LhaImmediateOutputStream out = new LhaImmediateOutputStream(Archive);
      byte[] buffer = new byte[8192];
      int length;
      long pos;
      if(oldArchive != null && headers != null) {
         for(int files = 0; files < headers.length; ++files) {
            if(headers[files] != null) {
               if(!param.omitProgressMessage) {
                  show(copyEntry.format(new Object[]{getDisplayFileName(param, headers[files].getPath())}) + "    ");
               }

               InputStream names = oldArchive.getInputStreamWithoutExtract(headers[files]);
               out.putNextEntryAlreadyCompressed(headers[files]);
               long i = 0L;
               long in = headers[files].getCompressedSize() / 10L;
               pos = i + in;

               while((length = names.read(buffer)) >= 0) {
                  out.write(buffer, 0, length);
                  if(pos <= (i += (long)length)) {
                     if(!param.omitProgressMessage) {
                        if(in < 1024L) { show(".");
                        } else if(in < 1048576L) {
                           show("o");
                        } else {
                           show("O");
                        }
                     }
                     pos += in;
                  }
               }
               names.close();
               out.closeEntry();
               if(!param.omitProgressMessage) {
                  show("  " + done + "\n");
               }
            }
         }
      }

      Vector arg25 = lists[0];
      Vector arg26 = lists[1];
      for(int arg27 = 0; arg27 < arg26.size(); ++arg27) {
         if(arg26.elementAt(arg27) != null) {
            File file = (File)arg25.elementAt(arg27);
            if(file.isDirectory()) {
               LhaHeader arg29 = new LhaHeader((String)arg26.elementAt(arg27));
               if(!param.omitProgressMessage) {
                  show(contEntry.format(new Object[]{getDisplayFileName(param, arg29.getPath())}) + "    ");
               }
               arg29.setHeaderLevel(param.HeaderLevel);
               arg29.setCompressMethod("-lhd-");
               arg29.setLastModified(new Date(file.lastModified()));
               arg29.setOriginalSize(0L);
               out.putNextEntry(arg29);
               out.closeEntry();
               if(!param.omitProgressMessage) {
                  show("  " + done + "\n");
               }            } else {
               RandomAccessFile arg28 = new RandomAccessFile(file, "r");
               LhaHeader header = new LhaHeader((String)arg26.elementAt(arg27));
               if(!param.omitProgressMessage) {
                  show(compEntry.format(new Object[]{getDisplayFileName(param, header.getPath())}) + "    ");
               }

               header.setHeaderLevel(param.HeaderLevel);
               header.setCompressMethod(param.Method);
               for(int arg30 = 0; arg30 < param.NoCompressExtentions.size(); ++arg30) {
                  if(param.NoCompressExtentions.elementAt(arg30).equals("")) {
                     if(file.getName().indexOf(44) == -1) {
                        header.setCompressMethod("-lh0-");
                     }
                  } else if(match(false, "*." + (String)param.NoCompressExtentions.elementAt(arg30), file.getName())) {
                     header.setCompressMethod("-lh0-");
                  }
               }
               header.setLastModified(new Date(file.lastModified()));
               header.setOriginalSize(arg28.length());
               out.putNextEntry(header);
               pos = 0L;
               long step = arg28.length() / 10L;
               long next = pos + step;

               while((length = arg28.read(buffer)) >= 0) {
                  out.write(buffer, 0, length);
                  if(next <= (pos += (long)length)) {
                     if(!param.omitProgressMessage) {
                        if(step < 1024L) {
                           show(".");
                        } else if(step < 1048576L) {
                           show("o");
                        } else {
                           show("O");
                        }
                     }
                     next += step;
                  }
               }

               if(!out.closeEntry()) {
                  if(!param.omitProgressMessage) {
                     show(failedComp + "    ");
                  }
                  arg28.seek(0L);
                  header.setCompressMethod("-lh0-");
                  out.putNextEntry(header);
                  pos = 0L;
                  next = pos + step;

                  while((length = arg28.read(buffer)) >= 0) {
                     out.write(buffer, 0, length);
                     if(next <= (pos += (long)length)) {
                        if(!param.omitProgressMessage) {
                           if(step < 1024L) {
                              show(".");
                           } else if(step < 1048576L) {
                              show("o");
                           } else {
                              show("O");
                           }
                        }
                        next += step;
                     } 
                  }
                  out.closeEntry();
               }
               arg28.close();
               if(!param.omitProgressMessage) {
                  show("  " + done + "\n");
               }
            }
         }
      }
      out.close();
   }

   private static final void copyTempToArc(LHA param) throws IOException {
      MessageFormat copyTempToArc = new MessageFormat(getLhaProperty("lha.message.copyTempToArc"));
      String done = getLhaProperty("lha.message.done");
      byte[] buffer = new byte[8192];

      param.Archive.delete();

      if(!param.Temporary.renameTo(param.Archive)) {
         if(!param.omitProgressMessage) {
            show(copyTempToArc.format(new Object[]{getDisplayFileName(param, param.Temporary.getPath()),
               getDisplayFileName(param, param.Archive.getPath())}) + "    ");
         }

         FileInputStream in = new FileInputStream(param.Temporary);
         FileOutputStream fout = new FileOutputStream(param.Archive);
         long pos = 0L;
         long step = param.Temporary.length() / 10L;
         long next = pos + step;
         int length;
         while((length = in.read(buffer)) >= 0) {
            fout.write(buffer, 0, length);
            if(next <= (pos += (long)length)) {
               if(!param.omitProgressMessage) {
                  if(step < 1024L) {
                     show(".");
                  } else if(step < 1048576L) {
                     show("o");
                  } else {
                     show("O");
                  }
               }
               next += step;
            }
         }

         param.Temporary.delete();
         if(!param.omitProgressMessage) {
            show("  " + done + "\n");
         }
      }
   }


   public static final void Add(LHA param) throws IOException {
      MessageFormat updateArc = new MessageFormat(getLhaProperty("lha.message.updateArc"));
      MessageFormat createArc = new MessageFormat(getLhaProperty("lha.message.createArc"));
      LhaFile archive;
      LhaHeader[] headers;
      if(param.Archive.isFile()) {
         archive = new LhaFile(param.Archive);
         headers = archive.getEntries();
         param.Temporary = getTempFile(param.Temporary);
         if(!param.omitProgressMessage) {
            show("\n" + updateArc.format(new Object[]{getDisplayFileName(param, param.Archive.getPath()),
                getDisplayFileName(param, param.Temporary.getPath())}) + "\n\n");

         }
      } else {
         archive = null;
         headers = new LhaHeader[0];
         if(!param.omitProgressMessage) {
            show("\n" + createArc.format(new Object[]{getDisplayFileName(param, param.Archive.getPath())}) + "\n\n");
         }
      }

      Vector[] lists = createFileList(param);
      Vector files = lists[0];
      Vector names = lists[1];
      int i;
      for(i = 0; i < names.size(); ++i) {
         for(int j = 0; j < headers.length; ++j) {
            if(names.elementAt(i) != null && headers[j] != null && ((String)names.elementAt(i)).equals(headers[j].getPath())) {
               if(!param.omitTimeCheck && headers[j].getLastModified().getTime() >= ((File)files.elementAt(i)).lastModified()) {
               } else {
                  headers[j] = null;
                  names.setElementAt((Object)null, i);
                  files.setElementAt((Object)null, i);
               }
            }
         }
      }

      if(archive == null) {
         param.Temporary = param.Archive;
      }
      buildArchive(param, param.Temporary, lists, archive, headers);

      if(archive != null) {
         archive.close();
         archive = null;
      }

      if(param.Command == 77) {
         for(i = 0; i < files.size(); ++i) {
            if(files.elementAt(i) != null) {
               ((File)files.elementAt(i)).delete();
            }         }      }
      if(param.Temporary != param.Archive) {
         copyTempToArc(param);      }
   }


   public static final void Freshen(LHA param) throws IOException {
      MessageFormat updateArc = new MessageFormat(getLhaProperty("lha.message.updateArc"));
      MessageFormat notfoundArc = new MessageFormat(getLhaProperty("lha.message.notfoundArc"));
      LhaFile archive = null;
      LhaHeader[] headers = null;

      if(param.Archive.isFile()) {
         archive = new LhaFile(param.Archive);
         headers = archive.getEntries();
         param.Temporary = getTempFile(param.Temporary);
         if(!param.omitProgressMessage) {
            show("\n" + updateArc.format(new Object[]{getDisplayFileName(param, param.Archive.getPath()),
                getDisplayFileName(param, param.Temporary.getPath())}) + "\n\n");
         }
      } else {
         show("\n" + notfoundArc.format(new Object[]{getDisplayFileName(param, param.Archive.getPath())}) + "\n");
         System.exit(0);
      }

      Vector[] lists = createFileList(param);
      Vector files = lists[0];
      Vector names = lists[1];
      Object[] list = new Object[headers.length];
      int i;
      for(i = 0; i < list.length; ++i) {
         list[i] = headers[i];
         long time = headers[i].getLastModified().getTime();

         for(int j = 0; j < names.size(); ++j) {
            if(names.elementAt(j) != null && ((String)names.elementAt(j)).equals(headers[i].getPath()) &&
             (param.omitTimeCheck || files.elementAt(j) != null && time < ((File)files.elementAt(j)).lastModified())) {
               list[i] = new Object[]{names.elementAt(i), files.elementAt(j)};
               names.setElementAt((Object)null, j);
               files.setElementAt((Object)null, j);
            }
         }
      }

      headers = new LhaHeader[headers.length];
      files.removeAllElements();
      names.removeAllElements();

      for(i = 0; i < list.length; ++i) {
         if(list[i] instanceof LhaHeader) {
            headers[i] = (LhaHeader)list[i];
         } else {
            headers[i] = null;
            names.addElement(((Object[])list[i])[0]);
            files.addElement(((Object[])list[i])[1]);
         }      }

      buildArchive(param, param.Temporary, lists, archive, headers);

      archive.close();
      archive = null;
      param.Archive.delete();
      copyTempToArc(param);
   }

   public static final void Delete(LHA param) throws IOException {
      MessageFormat updateArc = new MessageFormat(getLhaProperty("lha.message.updateArc"));
      MessageFormat notfoundArc = new MessageFormat(getLhaProperty("lha.message.notfoundArc"));

      LhaFile archive = null;
      LhaHeader[] headers = null;
      if(param.Archive.isFile()) {
         archive = new LhaFile(param.Archive);
         headers = archive.getEntries();
         param.Temporary = getTempFile(param.Temporary);
         if(!param.omitProgressMessage) {
            show("\n" + updateArc.format(new Object[]{getDisplayFileName(param, param.Archive.getPath()),
               getDisplayFileName(param, param.Temporary.getPath())}) + "\n\n");

         }
      } else {
         show(notfoundArc.format(new Object[]{getDisplayFileName(param, param.Archive.getPath())}));
         System.exit(0);
      }

      for(int i = 0; i < headers.length; ++i) {
         for(int j = 0; j < param.FileNames.size(); ++j) {
            if(match(false, getFileName(headers[i].getPath()), getFileName((String)param.FileNames.elementAt(j)))) {
               headers[i] = null;
               break;
            }
         }
      }

      buildArchive(param, param.Temporary, new Vector[]{new Vector(), new Vector()}, archive, headers);

      archive.close();
      archive = null;

      copyTempToArc(param);
   }

   public static final void Extract(LHA param) throws IOException {
      MessageFormat extracArc = new MessageFormat(getLhaProperty("lha.message.extracArc"));
      MessageFormat extrEntry = new MessageFormat(getLhaProperty("lha.message.extrEntry"));
      MessageFormat failedMkdir = new MessageFormat(getLhaProperty("lha.message.failedMkdir"));
      MessageFormat existFile = new MessageFormat(getLhaProperty("lha.message.existFile"));
      MessageFormat notfoundArc = new MessageFormat(getLhaProperty("lha.message.notfoundArc"));

      String failedCRC = getLhaProperty("lha.message.failedCRC");
      String failedLength = getLhaProperty("lha.message.failedLength");
      String failedSetTime = getLhaProperty("lha.message.failedSetTime");
      String existDir = getLhaProperty("lha.message.existDir");
      String existNewest = getLhaProperty("lha.message.existNewest");
      String giveupExtr = getLhaProperty("lha.message.giveupExtr");
      String emptyDir = getLhaProperty("lha.message.emptyDir");

      String askContinue = getLhaProperty("lha.message.askContinue");
      String askAnother = getLhaProperty("lha.message.askAnother");
      String askNewName = getLhaProperty("lha.message.askNewName");
      String askDelete = getLhaProperty("lha.message.askDelete");

      String skip = getLhaProperty("lha.message.skip");
      String done = getLhaProperty("lha.message.done");
      String fail = getLhaProperty("lha.message.fail");

      if(!param.Archive.isFile()) {
         show("\n" + notfoundArc.format(new Object[]{param.showWithDirectory?
            param.Archive.getPath():param.Archive.getName()}) + "\n\n");
         System.exit(0);
      }

      if(!param.omitProgressMessage) {
         show("\n" + extracArc.format(new Object[]{param.showWithDirectory?
            param.Archive.getPath():param.Archive.getName()}) + "\n\n");
      }

      LhaFile archive = new LhaFile(param.Archive);
      LhaHeader[] headers = archive.getEntries();
      CRC16 crc = new CRC16();

      for(int i = 0; i < headers.length; ++i) {
         boolean hit = false;
         String base = null;

         String filename = headers[i].getPath();
         if(!param.strictlyPathCheck) {
            filename = getFileName(filename);
         }
         for(int dest = 0; dest < param.FileNames.size(); ++dest) {
            if(!hit && match(false, (String)param.FileNames.elementAt(dest), filename)) {
               hit = true;
               base = (String)param.BasePaths.elementAt(dest);
            }
         }
         if(hit) {
            File arg38 = new File(base, param.withDirectoryStructure?headers[i].getPath():getFileName(headers[i].getPath()));
            arg38 = new File(arg38.getAbsolutePath());
            File parent = new File(arg38.getParent());
            if(!parent.isDirectory() && !parent.mkdirs()) {
               if(!param.omitProgressMessage) {
                  show(failedMkdir.format(new Object[]{parent.getAbsolutePath()}) + "\n");
               }
               if(param.omitQuestion && question(askContinue)) {
                  continue;
               }
               System.exit(0);
            }

            show(extrEntry.format(new Object[]{getDisplayFileName(param, headers[i].getPath())}) + "    ");

            if(headers[i].getCompressMethod().equals("-lhd-")) {
               if(param.withDirectoryStructure) {
                  if(arg38.mkdir()) {
                     if(!param.omitProgressMessage) {
                        if(setLastModified(arg38, headers[i].getLastModified().getTime())) {
                           show("  " + done + "\n");
                        } else {
                           show("  " + done + " " + failedSetTime + "\n");
                        }                     }                  } else {
                     if(!param.omitProgressMessage) {
                        show(fail + "\n" + failedMkdir.format(new Object[]{arg38.getAbsolutePath()}) + "\n");                     }
                     if(!param.omitQuestion && !question(askContinue)) {

                        System.exit(0);
                     }
                  }
               } else if(!param.omitProgressMessage) {
                  show(skip + " " + emptyDir + "\n");
               }
            } else {
               if(arg38.isDirectory()) {
                  if(!param.omitProgressMessage) {
                     show(fail + "\n" + existDir + "\n");
                  }
                  if(param.omitQuestion && !question(askAnother)) {
                     continue;
                  }
               }
                  String out = askString(askNewName);
                  if(out != null) {
                     arg38 = new File(arg38.getParent(), out);
                  }
                  while(out != null && arg38.exists()) {
                     if(!param.omitProgressMessage) {
                        show(existFile.format(new Object[]{arg38.getPath()}) + "\n");
                     }
                     out = askString(askNewName);
                     if(out != null) {
                        arg38 = new File(arg38.getParent(), out);
                     }
                  }
                  if(out == null) {
                     if(!param.omitProgressMessage) {
                        show(giveupExtr + "\n");
                     }
                     continue;
                  }
                  if(!param.omitProgressMessage) {
                     show(extrEntry.format(new Object[]{getDisplayFileName(
                           param, getDirName(headers[i].getPath()) + File.separator + out)}) + "   ");
                     if(!param.omitProgressMessage) {
                        show(giveupExtr + "\n");
                     }
                  }

               if(!param.omitTimeCheck && arg38.exists() && headers[i].getLastModified().getTime() >= arg38.lastModified()) {
                  
               } else {
                  FileOutputStream arg39 = new FileOutputStream(arg38);
                  InputStream in = archive.getInputStream(headers[i]);
                  byte[] buffer = new byte[8192];

                  long pos = 0L;
                  long step = headers[i].getOriginalSize() / 10L;
                  long next = pos + step;

                  crc.reset();                  int length;
                  while((length = in.read(buffer)) >= 0) {
                     arg39.write(buffer, 0, length);
                     crc.update(buffer, 0, length);
                     if(next <= (pos += (long)length)) {
                        if(!param.omitProgressMessage) {
                           if(step < 1024L) {  
                              show(".");
                           } else if(step < 1048576L) {
                              show("o");
                           } else {
                              show("O");
                           }
                        }
                        next += step;
                     }
                  }

                  arg39.close();
                  if((long)headers[i].getCRC() == crc.getValue() && headers[i].getOriginalSize() == pos) {
                     if(!param.omitProgressMessage) {
                        if(setLastModified(arg38, headers[i].getLastModified().getTime())) {
                           show("  " + done + "\n");
                        } else {
                           show("  " + done + " " + failedSetTime + "\n");
                        }                     
                     }
                  } else {
                     if(!param.omitProgressMessage) {
                        show("  " + (headers[i].getOriginalSize() == pos?failedCRC:failedLength) + "\n");
                     }
                     if(param.omitQuestion || question(askDelete)) {
                        arg38.delete();
                     }
                     if(!param.omitQuestion && !question(askContinue)) {
                        System.exit(0);
                     }
                  }
                  if(!param.omitProgressMessage) {
                     show(skip + " " + existNewest + "\n");
                  }
               }
            }
         }
      }
   }


   public static final void Test(LHA param) throws IOException {
      MessageFormat testinArc = new MessageFormat(getLhaProperty("lha.message.testinArc"));
      MessageFormat testEntry = new MessageFormat(getLhaProperty("lha.message.testEntry"));
      String failedCRC = getLhaProperty("lha.message.failedCRC");
      String failedLength = getLhaProperty("lha.message.failedLength");
      String askContinue = getLhaProperty("lha.message.askContinue");
      String done = getLhaProperty("lha.message.done");
      String fail = getLhaProperty("lha.message.fail");

      if(!param.omitProgressMessage) {
         show("\n" + testinArc.format(new Object[]{param.showWithDirectory?
            param.Archive.getPath():param.Archive.getName()}) + "\n\n");
      }

      LhaFile archive = new LhaFile(param.Archive);
      LhaHeader[] headers = archive.getEntries();
      CRC16 crc = new CRC16();

      for(int i = 0; i < headers.length; ++i) {
         boolean hit = false;
         String base = null;

         String filename = headers[i].getPath();
         if(!param.strictlyPathCheck) {
            filename = getFileName(filename);
         }
         for(int in = 0; in < param.FileNames.size(); ++in) {
            if(!hit && match(false, (String)param.FileNames.elementAt(in), filename)) {
               hit = true;
               base = (String)param.BasePaths.elementAt(in);
            }
         }
         if(hit) {
            if(!param.omitProgressMessage) {
               show(testEntry.format(new Object[]{getDisplayFileName(param, headers[i].getPath())}) + "    ");
            }

            InputStream arg23 = archive.getInputStream(headers[i]);
            byte[] buffer = new byte[8192];
            long pos = 0L;
            long step = headers[i].getOriginalSize() / 10L;
            long next = pos + step;
            crc.reset();
            int length;
            while((length = arg23.read(buffer)) >= 0) {
               crc.update(buffer, 0, length);
               if(next <= (pos += (long)length)) {
                  if(!param.omitProgressMessage) {
                     if(step < 1024L) {
                        show(".");
                     } else if(step < 1048576L) {
                        show("o");
                     } else {
                        show("O");
                     }
                  }
                  next += step;
               }
            }

            if((long)headers[i].getCRC() == crc.getValue() && headers[i].getOriginalSize() == pos) {
               if(!param.omitProgressMessage) {
                  show("  " + done + "\n");
               }
            } else if(!param.omitProgressMessage) {
               show("  " + (headers[i].getOriginalSize() == pos?failedCRC:failedLength) + "\n");

            } else {
               show(testEntry.format(new Object[]{getDisplayFileName(param, headers[i].getPath())}) +
                "  " + (headers[i].getOriginalSize() == pos?failedCRC:failedLength) + "\n");
            }
         }
      }

   }

   public static final void Print(LHA param) throws IOException {
      MessageFormat printHeader = new MessageFormat(getLhaProperty("lha.format.printHeader"));
      MessageFormat printFooter = new MessageFormat(getLhaProperty("lha.format.printFooter"));
      String failedCRC = getLhaProperty("lha.message.failedCRC");
      String failedLength = getLhaProperty("lha.message.failedLength");
      String askContinue = getLhaProperty("lha.message.askContinue");

      LhaFile archive = new LhaFile(param.Archive);
      LhaHeader[] headers = archive.getEntries();
      CRC16 crc = new CRC16();

      for(int i = 0; i < headers.length; ++i) {
         boolean hit = false;
         String base = null;
         String filename = headers[i].getPath();
         if(!param.strictlyPathCheck) {
            filename = getFileName(filename);
         }
         for(int in = 0; in < param.FileNames.size(); ++in) {
            if(!hit && match(false, (String)param.FileNames.elementAt(in), filename)) {
               hit = true;
               base = (String)param.BasePaths.elementAt(in);
            }
         }

         if(hit) {
            System.out.print(printHeader.format(new Object[]{headers[i].getPath()}));
            InputStream arg17 = archive.getInputStream(headers[i]);
            byte[] buffer = new byte[8192];
            long pos = 0L;
            crc.reset();
            int length;
            while((length = arg17.read(buffer)) >= 0) {
               System.out.write(buffer, 0, length);
               crc.update(buffer, 0, length);
               pos += (long)length;
            }

            if((long)headers[i].getCRC() == crc.getValue() && headers[i].getOriginalSize() == pos) {

               System.out.print(printFooter.format(new Object[]{headers[i].getPath()}));
            } else {
               show((headers[i].getOriginalSize() == pos?failedCRC:failedLength) + "\n");

               if(!param.omitQuestion && !question(askContinue)) {
                  System.exit(0);
               }
            }
         }
      }

   }


   public static final void List(LHA param) throws IOException {
      MessageFormat listArc = new MessageFormat(getLhaProperty("lha.message.listinArc"));
      MessageFormat notfoundArc = new MessageFormat(getLhaProperty("lha.message.notfoundArc"));
      MessageFormat list = new MessageFormat(getLhaProperty(param.Command == 76?"lha.list.pattern.short":"lha.list.pattern.long"));
      MessageFormat total = new MessageFormat(getLhaProperty(param.Command == 76?"lha.list.totalpattern.short":"lha.list.totalpattern.long"));
      String partition = getLhaProperty(param.Command == 76?"lha.list.partition.short":"lha.list.partition.long");
      String instruction = getLhaProperty(param.Command == 76?"lha.list.instruction.short":"lha.list.instruction.long");

      if(!param.Archive.isFile()) {
         show("\n" + notfoundArc.format(new Object[]{param.showWithDirectory?
               param.Archive.getPath():param.Archive.getName()}) + "\n\n");
         System.exit(0);
      }

      LhaFile archive = new LhaFile(param.Archive);
      LhaHeader[] headers = archive.getEntries();

      long totalFileNumber = 0L;
      long totalCompressedSize = 0L;
      long totalOriginalSize = 0L;
      Object[] arg = new Object[8];

      System.out.println();
      System.out.println(listArc.format(new Object[]{getDisplayFileName(param, param.Archive.getAbsolutePath())}));

      System.out.println();
      System.out.println(instruction);
      System.out.println(partition);
      for(int i = 0; i < headers.length; ++i) {
         boolean hit = false;
         String filename = headers[i].getPath().substring(headers[i].getPath().lastIndexOf(File.separatorChar) + 1);

         for(int j = 0; j < param.FileNames.size(); ++j) {
            if(match(false, (String)param.FileNames.elementAt(j), filename)) {
               hit = true;
            }
         }

         if(hit) {
            arg[1] = (!param.showWithDirectory && !headers[i].getPath().equals(filename)?"+":" ") +
               (param.showWithDirectory?headers[i].getPath():(headers[i].getCompressMethod().equals("-lhd-")?"<dir>":filename));
            arg[2] = headers[i].getCompressMethod();
            arg[3] = formatCRC16(headers[i].getCRC());
            arg[4] = new Long(headers[i].getCompressedSize());
            arg[5] = new Long(headers[i].getOriginalSize());
            if(headers[i].getOriginalSize() != 0L) {
               arg[6] = new Double((double)headers[i].getCompressedSize() / (double)headers[i].getOriginalSize());
            } else {
               arg[6] = new Double(1.0D);
            }
            arg[7] = headers[i].getLastModified();
            System.out.println(list.format(arg));

            ++totalFileNumber;
            totalCompressedSize += headers[i].getCompressedSize();
            totalOriginalSize += headers[i].getOriginalSize();
         }
      }
      System.out.println(partition);

      arg[0] = new Long(totalCompressedSize);
      arg[1] = new Long(totalOriginalSize);
      if(totalOriginalSize != 0L) {
         arg[2] = new Double((double)totalCompressedSize / (double)totalOriginalSize);

      } else {
         arg[2] = new Double(1.0D);
      }
      arg[3] = new Long(totalFileNumber);
      System.out.println(total.format(arg));
      System.out.println("");
   }

   private static final String formatCRC16(int crc) {
      char[] chars = new char[]{Character.toUpperCase(Character.forDigit(crc >> 12 & 15, 16)),
          Character.toUpperCase(Character.forDigit(crc >> 8 & 15, 16)),
          Character.toUpperCase(Character.forDigit(crc >> 4 & 15, 16)),
          Character.toUpperCase(Character.forDigit(crc & 15, 16))};
      return new String(chars);
   }

   private static final String getSystemProperty(String key) {
      String val = null;
      try {
         val = System.getProperty(key);
      } catch (SecurityException arg2) {
         ;
      }
      return val;
   }
}
