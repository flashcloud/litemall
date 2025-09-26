package org.linlinjava.litemall.db.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.boot.system.ApplicationHome;

import com.fasterxml.jackson.databind.ObjectMapper;

public class DbUtil {

    public static void backup(File file, String user, String password, String db) {
        try {
            Runtime rt = Runtime.getRuntime();
            String command = "mysqldump -u" + user + " -p" + password + " --set-charset=utf8 " + db;
            Process child = rt.exec(command);
            InputStream inputStream = child.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter( new FileOutputStream(file), StandardCharsets.UTF_8);
            String str;
            while ((str = bufferedReader.readLine()) != null) {
                outputStreamWriter.write(str + "\r\n");
            }
            outputStreamWriter.flush();
            inputStream.close();
            bufferedReader.close();
            outputStreamWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void load(File file, String user, String password, String db) {
        try {
            Runtime rt = Runtime.getRuntime();
            String command = "mysql -u" + user + " -p" + password + " --default-character-set=utf8 " + db;
            Process child = rt.exec(command);
            OutputStream outputStream = child.getOutputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
            String str;
            while ((str = bufferedReader.readLine()) != null) {
                outputStreamWriter.write(str + "\r\n");
            }
            outputStreamWriter.flush();
            outputStream.close();
            bufferedReader.close();
            outputStreamWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 读取jar文件同级目录下的JSON文件
     * @param fileName JSON文件名
     * @return JSON字符串
     */
    public static String readJsonFileFromJarDirectory(String fileName) {
        try {
            // 获取jar文件所在目录
            ApplicationHome home = new ApplicationHome(DbUtil.class);
            File jarFile = home.getSource();
            File jarDir = jarFile.getParentFile();
            
            // 构建JSON文件路径
            Path jsonFilePath = Paths.get(jarDir.getAbsolutePath(), fileName);
            
            if (Files.exists(jsonFilePath)) {
                return new String(Files.readAllBytes(jsonFilePath));
            } else {
                throw new RuntimeException("JSON文件不存在: " + jsonFilePath.toString());
            }
        } catch (IOException e) {
            throw new RuntimeException("读取JSON文件失败", e);
        }
    }

    /**
     * 读取JSON文件并转换为指定对象类型
     * @param fileName JSON文件名
     * @param clazz 目标类型
     * @return 转换后的对象
     */
    public static <T> T readJsonFileAsObject(String fileName, Class<T> clazz, ObjectMapper objectMapper) {
        try {
            String jsonContent = readJsonFileFromJarDirectory(fileName);
            return objectMapper.readValue(jsonContent, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }    
}