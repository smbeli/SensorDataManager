package com.ubhave.datahandler.config;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;

import android.os.Environment;
import android.util.Log;

import com.ubhave.datahandler.except.DataHandlerException;

public class DataHandlerConfig
{
	private final static String TAG = "DataHandlerConfig";
	private static DataHandlerConfig instance;
	
	public final static String PRINT_LOG_D_MESSAGES = "PRINT_LOG_D_MESSAGES";
	private final static boolean DEFAULT_PRINT_LOG_D_MESSAGES = true;

	public static DataHandlerConfig getInstance()
	{
		if (instance == null)
		{
			instance = new DataHandlerConfig();
		}
		return instance;
	}

	private final HashSet<String> validKeys;
	private final HashMap<String, Object> config;

	public DataHandlerConfig()
	{
		config = new HashMap<String, Object>();
		config.putAll(DataStorageConfig.defaultValues());
		config.putAll(DataTransferConfig.defaultValues());
		config.putAll(FileSyncConfig.defaultValues());
		config.put(PRINT_LOG_D_MESSAGES, DEFAULT_PRINT_LOG_D_MESSAGES);

		validKeys = new HashSet<String>();
		validKeys.add(PRINT_LOG_D_MESSAGES);
		validKeys.addAll(DataStorageConfig.validKeys());
		validKeys.addAll(DataTransferConfig.validKeys());
		validKeys.addAll(FileSyncConfig.validKeys());
	}
	
	public static boolean shouldLog()
	{
		try
		{
			return (Boolean) getInstance().get(PRINT_LOG_D_MESSAGES);
		}
		catch (DataHandlerException e)
		{
			e.printStackTrace();
			return true;
		}
	}
	
	public String getLocalUploadDirectoryPath()
	{
		// TODO check permissions
		String absoluteDir = (String) config.get(DataStorageConfig.LOCAL_STORAGE_ROOT_NAME);
		return absoluteDir +"/"+ config.get(DataStorageConfig.LOCAL_STORAGE_UPLOAD_DIRECTORY_NAME);
	}

	public void setConfig(final String key, final Object value) throws DataHandlerException
	{
		if (validKeys.contains(key))
		{
			if (key.equals(DataStorageConfig.LOCAL_STORAGE_ROOT_NAME))
			{
				// TODO fix this
				String absoluteDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + (String) value;
				config.put(key,  absoluteDir);
			}
			else
			{
				config.put(key, value);
			}
		}
		else
		{
			throw new DataHandlerException(DataHandlerException.UNKNOWN_CONFIG);
		}
	}
	
	public boolean shouldUpload(final File file)
	{
		if (file.isFile())
		{
			String fileName = file.getName();
			return fileName.endsWith(DataStorageConstants.LOG_FILE_SUFFIX);
		}
		else
		{
			return false;
		}
	}

	public boolean containsConfig(final String key)
	{
		return config.containsKey(key);
	}

	public Object get(final String key) throws DataHandlerException
	{
		if (validKeys.contains(key))
		{
			return config.get(key);
		}
		else
		{
			System.err.println("Unknown config: "+key);
			throw new DataHandlerException(DataHandlerException.UNKNOWN_CONFIG);
		}
	}
	
	public String getIdentifier() throws DataHandlerException
	{
		String device_id = (String) get(DataStorageConfig.UNIQUE_DEVICE_ID);
		if (device_id == null)
		{
			String user_id = (String) get(DataStorageConfig.UNIQUE_USER_ID);
			if (user_id  == null)
			{
				if (shouldLog())
				{
					Log.d(TAG, "Error: user identifier is: "+user_id+", device identifier is: "+device_id);
				}
				throw new DataHandlerException(DataHandlerException.CONFIG_CONFLICT);
			}
			return user_id;
		}
		return device_id;
	}
}
