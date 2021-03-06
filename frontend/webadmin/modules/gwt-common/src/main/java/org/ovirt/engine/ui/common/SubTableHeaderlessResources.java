package org.ovirt.engine.ui.common;

import com.google.gwt.user.cellview.client.CellTable;

public interface SubTableHeaderlessResources extends CellTable.Resources {

    interface TableStyle extends CellTable.Style {
    }

    @Override
    @Source({ CellTable.Style.DEFAULT_CSS, "org/ovirt/engine/ui/common/css/TabCellTable.css",
    "org/ovirt/engine/ui/common/css/TabCellTableHeaderless.css" })
    TableStyle cellTableStyle();

}
