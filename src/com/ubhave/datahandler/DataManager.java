package com.ubhave.datahandler;

import java.io.File;
import java.util.HashMap;

import android.content.Context;
import android.util.Log;

import com.ubhave.dataformatter.DataFormatter;
import com.ubhave.datahandler.store.DataStorage;
import com.ubhave.datahandler.transfer.DataTransfer;
import com.ubhave.datahandler.transfer.WebConnection;
import com.ubhave.sensormanager.ESException;
import com.ubhave.sensormanager.data.SensorData;
import com.ubhave.triggermanager.TriggerException;

public class DataManager
{
	private static final String TAG = "DataManager";

	private static DataManager instance;

	private final DataHandlerConfig config;
	private final DataStorage storage;
	private final DataTransfer transfer;
	private final DataHandlerEventManager eventManager;

	private static final Object singletonLock = new Object();
	private final Object fileTransferLock = new Object();

	public static DataManager getInstance(final Context context) throws ESException, TriggerException
	{
		if (instance == null)
		{
			synchronized (singletonLock)
			{
				if (instance == null)
				{
					instance = new DataManager(context);
				}
			}
		}
		return instance;
	}

	private DataManager(final Context context) throws ESException, TriggerException
	{
		config = DataHandlerConfig.getInstance();
		storage = new DataStorage(context);
		transfer = new DataTransfer();
		eventManager = new DataHandlerEventManager(context, this);
	}

	public void setConfig(final String key, final Object value) throws DataHandlerException
	{
		config.setConfig(key, value);
		if (key.equals(DataHandlerConfig.DATA_POLICY))
		{
			eventManager.setPolicy((Integer) value);
		}
	}

	public void moveFileToUploadDir(File file)
	{
		synchronized (fileTransferLock)
		{
			File directory = new File(DataHandlerConfig.SERVER_UPLOAD_DIR);
			if (!directory.exists())
			{
				directory.mkdirs();
			}
			file.renameTo(new File(directory.getAbsolutePath() + "/" + file.getName()));
		}
	}

	public void transferStoredData()
	{
		synchronized (fileTransferLock)
		{
			File directory = new File(DataHandlerConfig.SERVER_UPLOAD_DIR);
			File[] files = directory.listFiles();
			for (File file : files)
			{
				HashMap<String, String> paramsMap = new HashMap<String, String>();
				paramsMap.put("password", "test");
				String response = WebConnection.postDataToServer("test_url", file, paramsMap);

				if (response.equals("success"))
				{
					Log.d(TAG, "file " + file + " successfully uploaded to the server");
					Log.d(TAG, "file " + file + " deleting local copy");
					file.delete();
				}
				else
				{
					Log.d(TAG, "file " + file + " failed to upload file to the server, response received: " + response);
				}
			}
		}
	}

	private boolean transferImmediately()
	{
		try
		{
			return ((Integer) config.get(DataHandlerConfig.DATA_POLICY)) == DataHandlerConfig.TRANSFER_IMMEDIATE;
		}
		catch (DataHandlerException e)
		{
			e.printStackTrace();
			return true;
		}
	}

	private DataFormatter getDataFormatter(int sensorType)
	{
		DataFormatter formatter = null;
		try
		{
			if (((Integer) config.get(DataHandlerConfig.DATA_FORMAT)) == DataHandlerConfig.JSON_FORMAT)
			{
				formatter = DataFormatter.getJSONFormatter(sensorType);
			}
			else
			{
				formatter = DataFormatter.getCSVFormatter(sensorType);
			}
		}
		catch (DataHandlerException e)
		{
			e.printStackTrace();
		}
		return formatter;
	}

	public void logSensorData(final SensorData data) throws DataHandlerException
	{
		DataFormatter formatter = getDataFormatter(data.getSensorType());
		if (transferImmediately())
		{
			transfer.postData(formatter.toString(data), (String) config.get(DataHandlerConfig.DATA_POST_TARGET_URL));
		}
		else
		{
			storage.logSensorData(data, formatter);
		}
	}

	public void logError(final String error) throws DataHandlerException
	{
		if (transferImmediately())
		{
			transfer.postError(error, (String) config.get(DataHandlerConfig.ERROR_POST_TARGET_URL));
		}
		else
		{
			storage.logError(error);
		}
	}

	public void logExtra(final String tag, final String data) throws DataHandlerException
	{
		if (transferImmediately())
		{
			transfer.postExtra(tag, data, (String) config.get(DataHandlerConfig.EXTRA_POST_TARGET_URL));
		}
		else
		{
			storage.logExtra(tag, data);
		}
	}
}
