/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.index.mapper;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.analysis.NamedAnalyzer;
import org.jetbrains.annotations.NotNull;

/** A {@link FieldMapper} for full-text fields. */
public class TextFieldMapper extends FieldMapper {

    public static final String CONTENT_TYPE = "text";

    public static final class Defaults {

        private Defaults() {}

        public static final FieldType FIELD_TYPE = new FieldType();

        static {
            FIELD_TYPE.setTokenized(true);
            FIELD_TYPE.setStored(false);
            FIELD_TYPE.setStoreTermVectors(false);
            FIELD_TYPE.setOmitNorms(false);
            FIELD_TYPE.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
            FIELD_TYPE.freeze();
        }

        /**
         * The default position_increment_gap is set to 100 so that phrase
         * queries of reasonably high slop will not match across field values.
         */
        public static final int POSITION_INCREMENT_GAP = 100;
    }

    public static class Builder extends FieldMapper.Builder {

        protected NamedAnalyzer indexAnalyzer;
        protected boolean omitNormsSet = false;
        private List<String> sources = new ArrayList<>();

        public Builder(String name) {
            super(name, Defaults.FIELD_TYPE);
        }

        @Override
        public void docValues(boolean docValues) {
            if (docValues) {
                throw new IllegalArgumentException("[text] fields do not support doc values");
            }
        }

        public void indexAnalyzer(NamedAnalyzer indexAnalyzer) {
            this.indexAnalyzer = indexAnalyzer;
        }

        public void omitNorms(boolean omitNorms) {
            this.fieldType.setOmitNorms(omitNorms);
            this.omitNormsSet = true;
        }

        public void storeTermVectors(boolean termVectors) {
            if (termVectors != this.fieldType.storeTermVectors()) {
                this.fieldType.setStoreTermVectors(termVectors);
            } // don't set it to false, it is default and might be flipped by a more specific option
        }

        public void storeTermVectorOffsets(boolean termVectorOffsets) {
            if (termVectorOffsets) {
                this.fieldType.setStoreTermVectors(termVectorOffsets);
            }
            this.fieldType.setStoreTermVectorOffsets(termVectorOffsets);
        }

        public void storeTermVectorPositions(boolean termVectorPositions) {
            if (termVectorPositions) {
                this.fieldType.setStoreTermVectors(termVectorPositions);
            }
            this.fieldType.setStoreTermVectorPositions(termVectorPositions);
        }

        public void storeTermVectorPayloads(boolean termVectorPayloads) {
            if (termVectorPayloads) {
                this.fieldType.setStoreTermVectors(termVectorPayloads);
            }
            this.fieldType.setStoreTermVectorPayloads(termVectorPayloads);
        }

        public void sources(List<String> sources) {
            this.sources = sources;
        }

        private TextFieldType buildFieldType(BuilderContext context) {
            TextFieldType ft = new TextFieldType(
                buildFullName(context),
                indexed,
                fieldType.indexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) >= 0);
            ft.setIndexAnalyzer(indexAnalyzer);
            return ft;
        }

        @Override
        public TextFieldMapper build(BuilderContext context) {
            TextFieldType tft = buildFieldType(context);
            var mapper = new TextFieldMapper(
                name,
                position,
                columnOID,
                isDropped,
                defaultExpression,
                fieldType,
                tft,
                copyTo,
                sources
            );
            context.putPositionInfo(mapper, position);
            return mapper;
        }


    }

    @NotNull
    private final List<String> sources;

    public static final class TextFieldType extends MappedFieldType {

        public TextFieldType(String name, boolean indexed, boolean hasPositions) {
            super(name, indexed, false, true);
            this.hasPositions = hasPositions;
        }

        public TextFieldType(String name) {
            this(name, true, true);
        }

        @Override
        public String typeName() {
            return CONTENT_TYPE;
        }
    }

    protected TextFieldMapper(String simpleName,
                              int position,
                              long columnOID,
                              boolean isDropped,
                              String defaultExpression,
                              FieldType fieldType,
                              TextFieldType mappedFieldType,
                              CopyTo copyTo,
                              List<String> sources) {
        super(simpleName, position, columnOID, isDropped, defaultExpression, fieldType, mappedFieldType, copyTo);
        assert mappedFieldType.hasDocValues() == false;
        this.sources = sources;
    }

    public List<String> sources() {
        return this.sources;
    }

    @Override
    protected TextFieldMapper clone() {
        return (TextFieldMapper) super.clone();
    }

    @Override
    protected String contentType() {
        return CONTENT_TYPE;
    }

    @Override
    protected void mergeOptions(FieldMapper other, List<String> conflicts) {
    }

    @Override
    public TextFieldType fieldType() {
        return (TextFieldType) super.fieldType();
    }

    @Override
    protected boolean docValuesByDefault() {
        return false;
    }

    @Override
    protected void doXContentBody(XContentBuilder builder, boolean includeDefaults) throws IOException {
        super.doXContentBody(builder, includeDefaults);
        doXContentAnalyzers(builder, includeDefaults);
        if (sources.isEmpty() == false) {
            builder.startArray("sources");
            for (String source : sources) {
                builder.value(source);
            }
            builder.endArray();
        }
    }
}
