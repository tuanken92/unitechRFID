package com.unitech.unitechrfidsample.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.unitech.lib.diagnositics.ReaderException;
import com.unitech.lib.htx.HT730Reader;
import com.unitech.lib.reader.BaseReader;
import com.unitech.lib.reader.event.IReaderEventListener;
import com.unitech.lib.reader.params.DisplayTags;
import com.unitech.lib.types.BeepAndVibrateState;
import com.unitech.lib.types.ReadOnceState;
import com.unitech.lib.reader.params.DisplayOutput;
import com.unitech.lib.reader.params.ScreenOffTime;
import com.unitech.lib.reader.types.BeeperState;
import com.unitech.lib.reader.types.KeyState;
import com.unitech.lib.reader.types.KeyType;
import com.unitech.lib.reader.types.NotificationState;
import com.unitech.lib.reader.types.VibratorState;
import com.unitech.lib.rgx.RG768Reader;
import com.unitech.lib.rpx.RP902Reader;
import com.unitech.lib.transport.TransportBluetooth;
import com.unitech.lib.transport.types.ConnectState;
import com.unitech.lib.types.ActionState;
import com.unitech.lib.types.DeviceType;
import com.unitech.lib.types.ResultCode;
import com.unitech.lib.uhf.BaseUHF;
import com.unitech.lib.uhf.event.IRfidUhfEventListener;
import com.unitech.lib.uhf.params.Lock6cParam;
import com.unitech.lib.uhf.params.PowerRange;
import com.unitech.lib.uhf.params.SelectMask6cParam;
import com.unitech.lib.uhf.params.TagExtParam;
import com.unitech.lib.uhf.types.AlgorithmType;
import com.unitech.lib.uhf.types.BLFType;
import com.unitech.lib.uhf.types.BankType;
import com.unitech.lib.uhf.types.LockState;
import com.unitech.lib.uhf.types.Mask6cAction;
import com.unitech.lib.uhf.types.Mask6cTarget;
import com.unitech.lib.uhf.types.PowerMode;
import com.unitech.lib.uhf.types.Session;
import com.unitech.lib.uhf.types.TARIType;
import com.unitech.lib.uhf.types.Target;
import com.unitech.lib.util.diagnotics.StringUtil;
import com.unitech.unitechrfidsample.ExtraName;
import com.unitech.unitechrfidsample.MainActivity;
import com.unitech.unitechrfidsample.R;
import com.unitech.unitechrfidsample.databinding.FragmentSampleBinding;
import com.unitech.unitechrfidsample.enums.FragmentType;
import com.unitech.unitechrfidsample.enums.IDType;
import com.unitech.unitechrfidsample.enums.UpdateUIType;

import org.tinylog.Logger;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import static com.unitech.lib.util.SysUtil.sleep;
import static com.unitech.unitechrfidsample.util.Utilities.updateUIText;

public class SampleFragment extends BaseFragment implements IReaderEventListener, IRfidUhfEventListener {
    private FragmentSampleBinding binding;
    public final int MAX_MASK = 2;
    private final int NIBLE_SIZE = 4;

    boolean accessTagResult = false;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        initFragment();

        binding = FragmentSampleBinding.inflate(inflater, container, false);

        //region Connect to the reader
        _connectExecutorService.execute(() -> {
            try {
                switch (_viewModel.deviceType) {
                    case Unknown:
                        break;
                    case RP902:
                        TransportBluetooth tb = new TransportBluetooth(DeviceType.RP902, "RP902", _viewModel.bluetoothMACAddress);
                        _activity.baseReader = new RP902Reader(tb);
                        _activity.baseReader.addListener(this);
                        _activity.baseReader.connect();

                        binding.layoutDisplay.setVisibility(View.VISIBLE);
                        break;
                    case HT730:
                        _activity.baseReader = new HT730Reader(_activity.getApplicationContext());
                        _activity.baseReader.addListener(this);
                        _activity.baseReader.connect();
                        break;
                    case RG768:
                        _activity.baseReader = new RG768Reader(_activity.getApplicationContext());
                        _activity.baseReader.addListener(this);
                        _activity.baseReader.connect();
                        break;
                }

            } catch (Exception e) {
                Logger.error(e.toString());
                MainActivity.showToast("Connect exception: " + e.toString());
            }
        });
        //endregion

        return binding.getRoot();
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonInfo.setOnClickListener(view1 -> {
            try {
                assertReader();
            } catch (Exception e) {
                MainActivity.showToast(e.getMessage());
                return;
            }

            StringBuilder data = new StringBuilder();

            try {
                data.append(getString(R.string.deviceName)).append(_activity.baseReader.getDeviceName()).append("\n");
                data.append(getString(R.string.sku)).append(_activity.baseReader.getSKU().toString()).append("\n");
                data.append(getString(R.string.region)).append(_activity.baseReader.getRfidUhf().getGlobalBand().toString()).append("\n");
                data.append(getString(R.string.version)).append(_activity.baseReader.getVersion()).append("\n");

                data.append(getString(R.string.temperature)).append(_activity.baseReader.getTemperature()).append("\n");

                if (_viewModel.deviceType == DeviceType.RP902) {
                    data.append(getString(R.string.time)).append(_activity.baseReader.getTime().toString()).append("\n");
                    data.append(getString(R.string.readMode)).append(_activity.baseReader.getReadMode().toString()).append("\n");
                    data.append(getString(R.string.operatingMode)).append(_activity.baseReader.getOperatingMode().toString()).append("\n");
                }
            } catch (ReaderException e) {
                e.printStackTrace();
            }

            MainActivity.showDialog("Info", data.toString());
        });

        binding.buttonSettings.setOnClickListener(view1 -> {
            try {
                assertReader();
            } catch (Exception e) {
                MainActivity.showToast(e.getMessage());
                return;
            }

            StringBuilder data = new StringBuilder();

            try {
                PowerRange powerRange = _activity.baseReader.getRfidUhf().getPowerRange();
                data.append(getString(R.string.power)).append(_activity.baseReader.getRfidUhf().getPower()).append(String.format(" (%d - %d)", powerRange.getMin(), powerRange.getMax())).append("\n");

                data.append(getString(R.string.inventoryTime)).append(_activity.baseReader.getRfidUhf().getInventoryTime()).append("\n");
                data.append(getString(R.string.idleTime)).append(_activity.baseReader.getRfidUhf().getIdleTime()).append("\n");

                data.append(getString(R.string.algorithm)).append(_activity.baseReader.getRfidUhf().getAlgorithmType().toString()).append("\n");
                data.append(getString(R.string.qValue));
                data.append(getString(R.string.startQ)).append(_activity.baseReader.getRfidUhf().getStartQ()).append(", ");
                data.append(getString(R.string.minQ)).append(_activity.baseReader.getRfidUhf().getMinQ()).append(", ");
                data.append(getString(R.string.maxQ)).append(_activity.baseReader.getRfidUhf().getMaxQ()).append("\n");

                data.append(getString(R.string.session)).append(_activity.baseReader.getRfidUhf().getSession().toString()).append(", ");
                data.append(getString(R.string.target)).append(_activity.baseReader.getRfidUhf().getTarget().toString()).append("\n");

                data.append(getString(R.string.toggleTarget)).append(_activity.baseReader.getRfidUhf().getToggleTarget()).append("\n");

                data.append(getString(R.string.continuousMode)).append(_activity.baseReader.getRfidUhf().getContinuousMode()).append("\n");

                switch (_viewModel.deviceType) {
                    case RP902:
                        data.append(getString(R.string.autoOffTime)).append(_activity.baseReader.getAutoOffTime()).append(Arrays.toString(_activity.baseReader.getAutoOffTimeList())).append("\n");

                        ScreenOffTime screenOff = _activity.baseReader.getScreenOffTime();
                        if (screenOff.getMinute() == 0) {
                            if (screenOff.getSecond() == 0)
                                data.append(getString(R.string.autoScreenOffTime)).append("disable").append("\n");
                            else
                                data.append(getString(R.string.autoScreenOffTime)).append(screenOff.getSecond()).append(" Secs").append("\n");
                        }
                        else {
                            data.append(getString(R.string.autoScreenOffTime)).append(screenOff.getMinute()).append(" Mins").append("\n");
                        }

                        data.append(getString(R.string.beep)).append(_activity.baseReader.getBeeper().toString()).append(", ");
                        data.append(getString(R.string.vibrator)).append(_activity.baseReader.getVibrator().toString()).append("\n");

                        data.append(getString(R.string.tari)).append(_activity.baseReader.getRfidUhf().getTARI().toString()).append(", ");
                        data.append(getString(R.string.blf)).append(_activity.baseReader.getRfidUhf().getBLF().toString()).append("\n");
                        data.append(getString(R.string.fastMode)).append(_activity.baseReader.getRfidUhf().getFastMode()).append("\n");
                        break;
                    case HT730:
                        data.append(getString(R.string.profile)).append(_activity.baseReader.getRfidUhf().getModuleProfile()).append("\n");
                        data.append(getString(R.string.powerMode)).append(_activity.baseReader.getRfidUhf().getPowerMode().toString()).append("\n");
                        break;
                }
            } catch (ReaderException e) {
                e.printStackTrace();
            }

            MainActivity.showDialog("Settings", data.toString());
        });

        binding.buttonInventory.setOnClickListener(view1 -> {
            try {
                assertReader();
            } catch (Exception e) {
                MainActivity.showToast(e.getMessage());
                return;
            }

            switch (_activity.baseReader.getAction()) {
                case Stop:
                    doInventory();
                    break;
                case Inventory6c:
                    doStop();
                    break;
            }
        });

        binding.buttonRead.setOnClickListener(view1 -> {
            try {
                assertReader();
            } catch (Exception e) {
                MainActivity.showToast(e.getMessage());
                return;
            }

            if (_activity.baseReader.getAction() == ActionState.Stop) {
                clearResult();
                doRead();
            }
        });

        binding.buttonWrite.setOnClickListener(view1 -> {
            try {
                assertReader();
            } catch (Exception e) {
                MainActivity.showToast(e.getMessage());
                return;
            }

            if (_activity.baseReader.getAction() == ActionState.Stop) {
                clearResult();
                doWrite();
            }
        });

        binding.buttonLock.setOnClickListener(view1 -> {
            try {
                assertReader();
            } catch (Exception e) {
                MainActivity.showToast(e.getMessage());
                return;
            }

            if (_activity.baseReader.getAction() == ActionState.Stop) {
                clearResult();
                Thread t = new Thread(new LockUnlockProc(this, true));
                t.start();
            }
        });

        binding.buttonUnlock.setOnClickListener(view1 -> {
            try {
                assertReader();
            } catch (Exception e) {
                MainActivity.showToast(e.getMessage());
                return;
            }

            if (_activity.baseReader.getAction() == ActionState.Stop) {
                clearResult();
                Thread t = new Thread(new LockUnlockProc(this, false));
                t.start();
            }
        });

        binding.buttonDisplay.setOnClickListener(view1 -> {
            try {
                assertReader();
            } catch (Exception e) {
                MainActivity.showToast(e.getMessage());
                return;
            }

            if (_activity.baseReader.getAction() == ActionState.Stop) {
                String str = String.valueOf(binding.editDisplay.getText());
                SetDisplayOutput(2, true, str);
            }
        });
    }

    private void SetDisplayOutput(int pLine, boolean bClear, String data) {
        final int MAX_CHARS = 16;
        DisplayOutput display = null;
        byte param = 0x00;

        if (data.length() < 16) {
            int pChar = (int)Math.floor((MAX_CHARS - data.length()) / 2);
            param |= (byte)pChar;
        }

        if (bClear) param |= (byte)0x20;

        if (pLine < 1 || pLine > 3) param |= (byte)0x80;
        else param |= (byte)pLine << 6;

        display = new DisplayOutput(param, data);

        try {
            _activity.baseReader.setDisplayOutput(display);
        } catch (ReaderException e) {
            Logger.error("SetDisplayOutput error");
            //throw new RuntimeException(e);
        }
    }

    @Override
    public void onPause() {
        if (_activity.baseReader != null) {
            if (_activity.baseReader.getAction() != ActionState.Stop) {
                _activity.baseReader.getRfidUhf().stop();
            }
        }

        super.onPause();
    }

    @Override
    public void receiveHandler(Bundle bundle) {
        UpdateUIType updateUIType = UpdateUIType.valueOf(bundle.getInt(ExtraName.Type));

        switch (updateUIType) {
            case Text: {
                String data = bundle.getString(ExtraName.Text);
                IDType idType = IDType.valueOf(bundle.getInt(ExtraName.TargetID));

                switch (idType) {
                    case ConnectState:
                        binding.connectState.setText(data);
                        break;
                    case Temperature:
                        binding.temperature.setText(data);
                        break;
                    case AccessResult:
                        binding.result.setText(data);
                        break;
                    case TagEPC:
                        binding.tagEPC.setText(data);
                        break;
                    case TagRSSI:
                        binding.tagRSSI.setText(data);
                        break;
                    case Battery:
                        binding.battery.setText(data);
                        break;
                    case Inventory:
                        binding.buttonInventory.setText(data);
                        break;
                    case Data:
                        binding.tagData.setText(data);
                        break;
                }
            }
            break;
        }
    }

    @Override
    public void onReaderActionChanged(BaseReader reader, ResultCode retCode, ActionState state, Object params) {
        if (state == ActionState.Inventory6c) {
            updateText(IDType.Inventory, getString(R.string.stop));
        } else if (state == ActionState.Stop) {
            updateText(IDType.Inventory, getString(R.string.inventory));
        }
    }

    @Override
    public void onReaderBatteryState(BaseReader reader, int batteryState, Object params) {
        updateText(IDType.Battery, String.valueOf(batteryState));
    }

    @Override
    public void onReaderKeyChanged(BaseReader reader, KeyType type, KeyState state, Object params) {

    }

    @Override
    public void onReaderStateChanged(BaseReader reader, ConnectState state, Object params) {
        updateText(IDType.ConnectState, state.toString());

        if (state == ConnectState.Connected) {
            if (_activity.baseReader.getRfidUhf() != null) {
                _activity.baseReader.getRfidUhf().addListener(this);
            }
        }
    }

    @Override
    public void onNotificationState(NotificationState state, Object params) {

    }

    @Override
    public void onReaderTemperatureState(BaseReader reader, double temperatureState, Object params) {
        updateText(IDType.Temperature, String.valueOf(temperatureState));
    }

    @Override
    public void onRfidUhfAccessResult(BaseUHF uhf, ResultCode code, ActionState action, String epc, String data, Object params) {
        if (code == ResultCode.NoError) {
            updateText(IDType.AccessResult, "Success");
        } else {
            updateText(IDType.AccessResult, code.toString());
        }

        if (StringUtil.isNullOrEmpty(data)) {
            updateText(IDType.Data, "");
        } else {
            updateText(IDType.Data, data);
        }
        accessTagResult = (code == ResultCode.NoError);
    }

    @Override
    public void onRfidUhfReadTag(BaseUHF uhf, String tag, Object params) {
        if (StringUtil.isNullOrEmpty(tag)) {
            return;
        }

        float rssi = 0;
        if (params != null) {
            TagExtParam param = (TagExtParam) params;
            rssi = param.getRssi();
        }

        updateText(IDType.TagEPC, tag);
        updateText(IDType.TagRSSI, String.valueOf(rssi));
    }

    private void doInventory() {
        try {
            initSetting();

            clearSelectMask();

            _activity.baseReader.setDisplayTags(new DisplayTags(ReadOnceState.Off, BeepAndVibrateState.On));
            _activity.baseReader.setSoftWareScan(true);
            _activity.baseReader.getRfidUhf().inventory6c();
        } catch (ReaderException e) {
            MainActivity.showToast(e.toString());
        }
    }

    private void doStop() {
        _activity.baseReader.setSoftWareScan(false);
        _activity.baseReader.getRfidUhf().stop();
    }

    private void doRead() {
        String targetTag = binding.tagEPC.getText().toString();

        try {
            assertTagEPC(targetTag);
        } catch (Exception e) {
            MainActivity.showToast(e.getMessage());
            return;
        }

        if (setSelectMask(targetTag)) {
            String accessPassword = "00000000";
            int offset = 2;
            int length = 6;

            if (!readTag(BankType.EPC, offset, length, accessPassword)) {
                MainActivity.showToast("Failed to read memory");
            }
        }
    }

    private void doWrite() {
        String targetTag = binding.tagEPC.getText().toString();

        try {
            assertTagEPC(targetTag);
        } catch (Exception e) {
            MainActivity.showToast(e.getMessage());
            return;
        }

        if (setSelectMask(targetTag)) {
            String accessPassword = "00000000";
            BankType bank = BankType.EPC;
            int offset = 2;

            //region Change the data for test
            if (targetTag.startsWith("1234")) {
                targetTag = "4321" + targetTag.substring(4);
            } else {
                targetTag = "1234" + targetTag.substring(4);
            }
            //endregion

            if (!writeTag(BankType.EPC, offset, accessPassword, targetTag)) {
                MainActivity.showToast("Failed to write memory");
            }
        }
    }

    class LockUnlockProc implements Runnable {
        SampleFragment _fragment;
        boolean _locked;

        LockUnlockProc(SampleFragment fragment, boolean locked) {
            this._fragment = fragment;
            this._locked = locked;
        }

        @Override
        public void run() {
            String targetTag = binding.tagEPC.getText().toString();

            try {
                SampleFragment.this.assertTagEPC(targetTag);
            } catch (Exception e) {
                MainActivity.showToast(e.getMessage());
                return;
            }

            if (SampleFragment.this.setSelectMask(targetTag)) {
                String accessPassword = "00000000";
                String data = "12345678";
                int offset = 2;

                //region Write the password for lock/unlock test
                accessTagResult = false;

                if (!SampleFragment.this.writeTag(BankType.Reserved, offset, accessPassword, data)) {
                    MainActivity.showToast("Write password fail");
                    return;
                }

                long startTime = System.currentTimeMillis();
                boolean timeout = false;

                while (SampleFragment.this._activity.baseReader.getAction() != ActionState.Stop) {
                    if (System.currentTimeMillis() - startTime > 3000) {
                        timeout = true;
                        break;
                    }
                    sleep(10);
                }

                if (timeout) {
                    MainActivity.showToast("Write password timeout");
                    return;
                }

                if (!accessTagResult) {
                    MainActivity.showToast("Write password fail from access result");
                    return;
                }
                //endregion

                accessPassword = data;

                Lock6cParam lockParam = new Lock6cParam();
                lockParam.setEpc(_locked ? LockState.Lock : LockState.Unlock);

                ResultCode res = _activity.baseReader.getRfidUhf().lock6c(lockParam, accessPassword);

                if (res != ResultCode.NoError) {
                    Logger.error("Failed to lock/unlock tag - " + res);
                }
            }
        }
    }

    /**
     * Read tag's memory
     *
     * @param bank     The BankType enumeration that specifies the Bank for reading memory.
     * @param offset   An integer specifying the starting address to start reading from the specified
     *                 bank. Unit is WORD unit.
     * @param length   An integer that specifies the length of data to read from the specified start
     *                 address. Unit is WORD unit.
     * @param password If the tag is locked, it is a hex string specifying the Access Password set
     *                 in the tag. Up to 8 characters (2 words) can be input.
     * @return
     */
    public boolean readTag(BankType bank, int offset, int length, String password) {
        ResultCode res = _activity.baseReader.getRfidUhf().readMemory6c(
                bank, offset, length, password);

        if (res != ResultCode.NoError) {
            Logger.error("Failed to read memory - " + res);
            return false;
        }
        return true;
    }

    /**
     * Write data to tag's memory
     *
     * @param bank     BankType enumeration that specifies the Bank for writing memory.
     * @param offset   An integer specifying the starting address to start writing from the specified
     *                 bank. Unit is WORD unit.
     * @param password If the tag is locked, it is a hex string specifying the Access Password set
     *                 in the tag. Up to 8 characters (2 words) can be input.
     * @param data     An Hex type string that specifies the data to be stored in memory from the
     *                 specified start address. Data must be specified in WORD units (4 characters).
     * @return
     */
    public boolean writeTag(BankType bank, int offset, String password, String data) {
        ResultCode res = _activity.baseReader.getRfidUhf().writeMemory6c(
                bank, offset, data, password);
        if (res != ResultCode.NoError) {
            Logger.error("Failed to write memory - " + res);
            return false;
        }
        return true;
    }

    /**
     * Select the target tag to access
     *
     * @param maskEpc The tag's EPC value
     * @return
     */
    public boolean setSelectMask(String maskEpc) {
        SelectMask6cParam param = new SelectMask6cParam(
                true,
                Mask6cTarget.SL,
                Mask6cAction.AB,
                BankType.EPC,
                0,
                maskEpc,
                maskEpc.length() * NIBLE_SIZE);
        try {
            for (int i = 0; i < MAX_MASK; i++) {
                _activity.baseReader.getRfidUhf().setSelectMask6cEnabled(i, false);
            }
            _activity.baseReader.getRfidUhf().setSelectMask6c(0, param);
            Logger.debug("setSelectMask success: " + param.toString());
        } catch (ReaderException e) {
            Logger.error("setSelectMask failed: \n" + e.getCode().getMessage());
            MainActivity.showToast("setSelectMask failed");
            return false;
        }
        return true;
    }

    /**
     * Clear selected tag
     *
     * @throws ReaderException
     */
    public void clearSelectMask() throws ReaderException {
        for (int i = 0; i < MAX_MASK; i++) {
            try {
                _activity.baseReader.getRfidUhf().setSelectMask6cEnabled(i, false);
                Logger.debug("clearSelectMask successful");
            } catch (ReaderException e) {
                throw e;
            }
        }
    }

    /**
     * Update the UI text view
     *
     * @param id   The ID to update
     * @param data The string to show
     */
    private void updateText(IDType id, String data) {
        updateUIText(FragmentType.SampleFragment, id.value(), data);
    }

    void clearResult() {
        updateText(IDType.AccessResult, "");
        updateText(IDType.Data, "");
    }

    void initSetting() {

        try {

            _activity.baseReader.getRfidUhf().setSession(Session.S0);
            _activity.baseReader.getRfidUhf().setContinuousMode(true);
            _activity.baseReader.getRfidUhf().setInventoryTime(200);
            _activity.baseReader.getRfidUhf().setIdleTime(20);


            _activity.baseReader.getRfidUhf().setAlgorithmType(AlgorithmType.DynamicQ);

            _activity.baseReader.getRfidUhf().setStartQ(4);
            _activity.baseReader.getRfidUhf().setMaxQ(15);
            _activity.baseReader.getRfidUhf().setMinQ(0);

            _activity.baseReader.getRfidUhf().setTarget(Target.A);

            _activity.baseReader.getRfidUhf().setToggleTarget(true);


            switch (_viewModel.deviceType) {
                case RP902:
                    _activity.baseReader.getRfidUhf().setPower(22);

                    _activity.baseReader.setScreenOffTime(new ScreenOffTime(2, 0));
                    _activity.baseReader.setAutoOffTime(2);

                    _activity.baseReader.setBeeper(BeeperState.Medium);
                    _activity.baseReader.setVibrator(VibratorState.On);

                    _activity.baseReader.getRfidUhf().setTARI(TARIType.T_25_00);
                    _activity.baseReader.getRfidUhf().setBLF(BLFType.BLF_256);
                    _activity.baseReader.getRfidUhf().setFastMode(true);

                    Date currentTime = Calendar.getInstance().getTime();
                    _activity.baseReader.setTime(currentTime);
                    break;
                case HT730:
                    _activity.baseReader.getRfidUhf().setPower(30);
                    _activity.baseReader.getRfidUhf().setModuleProfile(0);
                    _activity.baseReader.getRfidUhf().setPowerMode(PowerMode.Optimized);
                    break;
            }
        } catch (ReaderException e) {
            e.printStackTrace();
        }
    }

    private void assertReader() throws Exception {
        _activity.assertReader();
    }

    private void assertTagEPC(String epc) throws Exception {
        if (StringUtil.isNullOrEmpty(epc)) {
            Logger.error("EPC is empty");
            throw new Exception("EPC is empty");
        }
    }
}
