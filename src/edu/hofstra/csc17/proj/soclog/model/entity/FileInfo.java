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
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
        if (fileDescriptor == null) {
            throw new IllegalArgumentException("File descriptor cannot be null");
        }
        if (fileDescriptor < 0) {
            throw new IllegalArgumentException("File descriptor must be non-negative, got: " + fileDescriptor);
        }
    }

    private static String validatePermissions(String permissions) {
        if (permissions == null) {
            throw new IllegalArgumentException("Permissions cannot be null");
        }
        
        // Validate 3-digit octal format (e.g., 640, 755)
        if (!permissions.matches("^[0-7]{3}$")) {
            throw new IllegalArgumentException(
                "Permissions must be a 3-digit octal string (e.g., '640', '755'), got: " + permissions);
        }
        
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
