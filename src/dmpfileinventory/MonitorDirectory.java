package dmpfileinventory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MonitorDirectory
{
    private static String DB_CONN_URL = null;
    private static String DB_USER = null;
    private static String DB_PASS = null;
    //private static String SRC_FOLDER = "F:\\projects\\dmp\\testing\\src";
    private static String SRC_FOLDER = null;
    
    public static void main(String[] args)
    {
        readConfigFile();
        readDirectory();
    }
    
    private static void readConfigFile()
    {
        BufferedReader reader = null;
        String[] splitLine;
        
        try 
        {
            String absPath = new File(MonitorDirectory.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
            
            String line = null;
            //reader = new BufferedReader(new FileReader(absPath.substring(0, absPath.length() - "dmpFileInventory.jar".length())+ "/config.txt"));
            reader = new BufferedReader(new FileReader("F:\\projects\\dmp\\config.txt"));
                        
            while((line = reader.readLine()) != null)
            {
                splitLine = line.split("#");
                
                if("SRC_FOLDER".equals(splitLine[0]))
                {
                    SRC_FOLDER = splitLine[1];
                }
                else if("DB_CONN_URL".equals(splitLine[0]))
                {
                    DB_CONN_URL = splitLine[1];
                }
                else if("DB_USER".equals(splitLine[0]))
                {
                    DB_USER = splitLine[1];
                }
                else if("DB_PASS".equals(splitLine[0]))
                {
                    DB_PASS = splitLine[1];
                }
            }
            
            reader.close();
        } 
        catch (Exception ex) 
        {
            Logger.getLogger(MonitorDirectory.class.getName()).log(Level.SEVERE, null, ex);
        } 
        finally 
        {
            try 
            {
                reader.close();
            } 
            catch (IOException ex) 
            {
                Logger.getLogger(MonitorDirectory.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public static void readDirectory()
    {
        try 
        {
            //Path faxFolder = Paths.get("./fax/");
            Path faxFolder = Paths.get(SRC_FOLDER);
            WatchService watchService = FileSystems.getDefault().newWatchService();
            faxFolder.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
            
            boolean valid = true;
            String fileName = null;
            File file = null;
            BasicFileAttributes attrs = null;
            
            do 
            {
                WatchKey watchKey = watchService.take();
                
                for (WatchEvent event : watchKey.pollEvents()) 
                {
                    //WatchEvent.Kind kind = event.kind();
                    if (StandardWatchEventKinds.ENTRY_CREATE.equals(event.kind())) 
                    {
                        fileName = event.context().toString();
                        //System.out.println("File Name:" + fileName);
                        
                        file = new File(SRC_FOLDER + "\\" + fileName);
                        //System.out.println("File Size:" + file.length());
                        
                        //attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                        //FileTime time = attrs.creationTime();
                        
                        /*
                        String pattern = "yyyy-MM-dd HH:mm:ss";
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
                        String formatted = simpleDateFormat.format(new Date(file.lastModified()));
                        System.out.println( "The file creation date and time is: " + formatted );
                        */
                        
                        saveRegister(fileName, new Timestamp(file.lastModified()), file.length());
                    }
                }
                
                valid = watchKey.reset();
                
                TimeUnit.SECONDS.sleep(1);
                
            } while (valid);
        } 
        catch (IOException | InterruptedException ex) 
        {
            Logger.getLogger(MonitorDirectory.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void saveRegister(String fileName, Timestamp lastModified, long size)
    {
        try
        {
            Class.forName("com.mysql.jdbc.Driver");

            //Insert data
            try (Connection conn = DriverManager.getConnection(DB_CONN_URL, DB_USER, DB_PASS))
            {
                String query = " insert into file_register (name, last_modified_at, size, total_line, status, created_at, updated_at)"
                        + " values (?, ?, ?, ?, ?, ?, ?)";
                PreparedStatement preparedStmt = conn.prepareStatement(query);
                preparedStmt.setString(1, fileName);
                preparedStmt.setTimestamp(2, lastModified);
                preparedStmt.setLong(3, size);
                preparedStmt.setInt(4, 0);
                preparedStmt.setInt(5, FileStatus.NEW.ordinal());
                
                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                
                preparedStmt.setTimestamp(6, timestamp);
                preparedStmt.setTimestamp(7, timestamp);
                
                preparedStmt.execute();
            }
        }
        catch (ClassNotFoundException | SQLException ex)
        {
            System.out.println(ex.toString());
        }
    }
}
