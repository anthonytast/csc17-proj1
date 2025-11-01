package edu.hofstra.csc17.proj.soclog.model.entity;

import java.util.Objects;

/**
 * Metadata describing a file or resource referenced by an event.
 */
public final class FileInfo extends ObjectInfo {

    private final String path;
    private final Integer fileDescriptor;
    private final String permissions;

    public FileInfo(String path, Integer fileDescriptor, String permissions) {
        this.path = path;
        this.fileDescriptor = fileDescriptor;
        this.permissions = validatePermissions(permissions);
        // TODO: Add validation for file fields and canonical path normalization
    }

    private static String validatePermissions(String permissions) {

        // TODO: Add proper Unix permission validation (e.g., 640, 755)
        return permissions;
    }

    public String getPath() {
        return path;
    }

    public Integer getFileDescriptor() {
        return fileDescriptor;
    }

    public String getPermissions() {
        return permissions;
    }

    @Override
    public String getDisplayName() {
        if (path != null && !path.isEmpty()) {
            return path;
        }
        if (fileDescriptor != null) {
            return "fd:" + fileDescriptor;
        }
        return "<unknown-file>";
    }

    @Override
    public String getCanonicalId() {
        if (path != null && !path.isEmpty()) {
            return "file:" + path;
        }
        if (fileDescriptor != null) {
            return "fd:" + fileDescriptor;
        }
        return "unknown-file";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FileInfo fileInfo = (FileInfo) o;
        return Objects.equals(path, fileInfo.path)
                && Objects.equals(fileDescriptor, fileInfo.fileDescriptor)
                && Objects.equals(permissions, fileInfo.permissions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, fileDescriptor, permissions);
    }

    @Override
    public String toString() {
        return "FileInfo{"
                + "path='" + path + '\''
                + ", fileDescriptor=" + fileDescriptor
                + ", permissions='" + permissions + '\''
                + '}';
    }
}
