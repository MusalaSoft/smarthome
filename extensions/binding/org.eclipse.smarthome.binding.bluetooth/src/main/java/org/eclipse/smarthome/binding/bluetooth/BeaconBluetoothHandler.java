package org.eclipse.smarthome.binding.bluetooth;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.binding.bluetooth.notification.BluetoothConnectionStatusNotification;
import org.eclipse.smarthome.binding.bluetooth.notification.BluetoothScanNotification;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.BridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.UnDefType;

/**
 * This is a handler for generic Bluetooth devices in beacon-mode (i.e. not connected), which at the same time can be
 * used as a base implementation for more specific thing handlers.
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
 */
public class BeaconBluetoothHandler extends BaseThingHandler implements BluetoothDeviceListener {

    protected BluetoothAdapter adapter;
    protected BluetoothAddress address;
    protected BluetoothDevice device;

    public BeaconBluetoothHandler(@NonNull Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        try {
            address = new BluetoothAddress(getConfig().get(BluetoothBindingConstants.CONFIGURATION_ADDRESS).toString());
        } catch (IllegalArgumentException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.getLocalizedMessage());
            return;
        }

        Bridge bridge = getBridge();
        if (bridge == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Not associated with any bridge");
            return;
        }

        BridgeHandler bridgeHandler = bridge.getHandler();
        if (!(bridgeHandler instanceof BluetoothAdapter)) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Associated with an unsupported bridge");
            return;
        }

        adapter = (BluetoothAdapter) bridgeHandler;
        device = adapter.getDevice(address);
        device.addListener(this);

        updateStatus(ThingStatus.UNKNOWN);
    }

    @Override
    public void dispose() {
        if (device != null) {
            device.removeListener(this);
            device = null;
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType && channelUID.getId().equals(BluetoothBindingConstants.CHANNEL_TYPE_RSSI)) {
            Integer rssi = device.getRssi();
            updateRSSI(rssi);
        }
    }

    /**
     * Updates the RSSI channel and the Thing status according to the new received rssi value
     *
     * @param rssi the received value
     */
    protected void updateRSSI(Integer rssi) {
        if (rssi != null && rssi != 0) {
            updateState(BluetoothBindingConstants.CHANNEL_TYPE_RSSI, new DecimalType(rssi));
            updateStatus(ThingStatus.ONLINE);
        } else {
            updateState(BluetoothBindingConstants.CHANNEL_TYPE_RSSI, UnDefType.NULL);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
        }
    }

    @Override
    public void onScanRecordReceived(BluetoothScanNotification scanNotification) {
        int rssi = scanNotification.getRssi();
        updateRSSI(rssi);
    }

    @Override
    public void onConnectionStateChange(@NonNull BluetoothConnectionStatusNotification connectionNotification) {
    }

    @Override
    public void onServicesDiscovered() {
    }

    @Override
    public void onCharacteristicReadComplete(@NonNull BluetoothCharacteristic characteristic,
            @NonNull BluetoothCompletionStatus status) {
    }

    @Override
    public void onCharacteristicWriteComplete(@NonNull BluetoothCharacteristic characteristic,
            @NonNull BluetoothCompletionStatus status) {
    }

    @Override
    public void onCharacteristicUpdate(@NonNull BluetoothCharacteristic characteristic) {
    }

}