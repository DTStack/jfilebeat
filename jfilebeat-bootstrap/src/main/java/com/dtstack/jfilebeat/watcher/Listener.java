package com.dtstack.jfilebeat.watcher;

import java.io.File;

public interface Listener {
	
    void onFileCreate(final File file);

    void onFileChange(final File file);

    void onFileDelete(final File file);

}
