package org.ovirt.engine.ui.uicommonweb.models.storage;

import java.util.ArrayList;

import org.ovirt.engine.core.common.businessentities.storage.LUNs;
import org.ovirt.engine.core.common.businessentities.storage.LunStatus;
import org.ovirt.engine.core.common.utils.ObjectUtils;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;

@SuppressWarnings("unused")
public class LunModel extends EntityModel<LUNs>
{

    private String lunId;

    public String getLunId()
    {
        return lunId;
    }

    public void setLunId(String value)
    {
        if (!ObjectUtils.objectsEqual(lunId, value))
        {
            lunId = value;
            onPropertyChanged(new PropertyChangedEventArgs("LunId")); //$NON-NLS-1$
        }
    }

    private String vendorId;

    public String getVendorId()
    {
        return vendorId;
    }

    public void setVendorId(String value)
    {
        if (!ObjectUtils.objectsEqual(vendorId, value))
        {
            vendorId = value;
            onPropertyChanged(new PropertyChangedEventArgs("VendorId")); //$NON-NLS-1$
        }
    }

    private String productId;

    public String getProductId()
    {
        return productId;
    }

    public void setProductId(String value)
    {
        if (!ObjectUtils.objectsEqual(productId, value))
        {
            productId = value;
            onPropertyChanged(new PropertyChangedEventArgs("ProductId")); //$NON-NLS-1$
        }
    }

    private String serial;

    public String getSerial()
    {
        return serial;
    }

    public void setSerial(String value)
    {
        if (!ObjectUtils.objectsEqual(serial, value))
        {
            serial = value;
            onPropertyChanged(new PropertyChangedEventArgs("Serial")); //$NON-NLS-1$
        }
    }

    private int size;

    public int getSize()
    {
        return size;
    }

    public void setSize(int value)
    {
        if (size != value)
        {
            size = value;
            onPropertyChanged(new PropertyChangedEventArgs("Size")); //$NON-NLS-1$
        }
    }

    private int multipathing;

    public int getMultipathing()
    {
        return multipathing;
    }

    public void setMultipathing(int value)
    {
        if (multipathing != value)
        {
            multipathing = value;
            onPropertyChanged(new PropertyChangedEventArgs("Multipathing")); //$NON-NLS-1$
        }
    }

    private boolean isAccessible;

    public boolean getIsAccessible()
    {
        return isAccessible;
    }

    public void setIsAccessible(boolean value)
    {
        if (isAccessible != value)
        {
            isAccessible = value;
            onPropertyChanged(new PropertyChangedEventArgs("IsAccessible")); //$NON-NLS-1$
        }
    }

    private boolean isIncluded;

    public boolean getIsIncluded()
    {
        return isIncluded;
    }

    public void setIsIncluded(boolean value)
    {
        if (isIncluded != value)
        {
            isIncluded = value;
            onPropertyChanged(new PropertyChangedEventArgs("IsIncluded")); //$NON-NLS-1$
        }
    }

    private boolean isGrayedOut;

    public boolean getIsGrayedOut()
    {
        return isGrayedOut;
    }

    public void setIsGrayedOut(boolean value)
    {
        if (isGrayedOut != value)
        {
            isGrayedOut = value;
            onPropertyChanged(new PropertyChangedEventArgs("IsGrayedOut")); //$NON-NLS-1$
        }
    }

    private ArrayList<String> grayedOutReasons;

    public ArrayList<String> getGrayedOutReasons()
    {
        return grayedOutReasons;
    }

    public void setGrayedOutReasons(ArrayList<String> value)
    {
        if (grayedOutReasons != value)
        {
            grayedOutReasons = value;
            onPropertyChanged(new PropertyChangedEventArgs("GrayedOutReasons")); //$NON-NLS-1$
        }
    }

    private ArrayList<SanTargetModel> targets;

    public ArrayList<SanTargetModel> getTargets()
    {
        return targets;
    }

    public void setTargets(ArrayList<SanTargetModel> value)
    {
        if (targets != value)
        {
            targets = value;
            onPropertyChanged(new PropertyChangedEventArgs("Targets")); //$NON-NLS-1$
            getTargetsList().setItems(targets);
        }
    }

    private ListModel targetsList;

    public ListModel getTargetsList()
    {
        return targetsList;
    }

    public void setTargetsList(ListModel value)
    {
        if (targetsList != value)
        {
            targetsList = value;
            onPropertyChanged(new PropertyChangedEventArgs("TargetsList")); //$NON-NLS-1$
        }
    }

    private LunStatus status;

    public LunStatus getStatus() {
        return status;
    }

    public void setStatus(LunStatus status) {
        this.status = status;
    }

    @Override
    public void setIsSelected(boolean value)
    {
        if (getIsGrayedOut()) {
            return;
        }

        super.setIsSelected(value);
    }

    public LunModel()
    {
        setTargetsList(new ListModel());
        setGrayedOutReasons(new ArrayList<String>());
    }

}
