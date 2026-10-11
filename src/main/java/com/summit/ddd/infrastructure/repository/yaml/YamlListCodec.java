package com.summit.ddd.infrastructure.repository.yaml;

import com.summit.ddd.infrastructure.repository.file.FileListCodec;

/** Retains the YAML codec contract for existing callers. */
public interface YamlListCodec<P> extends FileListCodec<P> {}
