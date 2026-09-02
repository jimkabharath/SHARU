package com.sharu.crm.storage;
import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
public class FileManager { public static boolean appendToCli(Context c, String name, String data) { try { File f = new File(c.getFilesDir(), name); FileOutputStream fos = new FileOutputStream(f, true); FileChannel ch = fos.getChannel(); ch.write(ByteBuffer.wrap(data.getBytes(StandardCharsets.UTF_8))); fos.close(); return true; } catch(Exception e) { return false; } } }