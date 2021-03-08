package com.gaoding.datay.core.transport.transformer;

import java.util.Map;

import com.gaoding.datay.common.element.Record;
import com.gaoding.datay.transformer.ComplexTransformer;
import com.gaoding.datay.transformer.Transformer;

/**
 * no comments.
 * Created by liqiang on 16/3/8.
 */
public class ComplexTransformerProxy extends ComplexTransformer {
    private Transformer realTransformer;

    public ComplexTransformerProxy(Transformer transformer) {
        setTransformerName(transformer.getTransformerName());
        this.realTransformer = transformer;
    }

    @Override
    public Record evaluate(Record record, Map<String, Object> tContext, Object... paras) {
        return this.realTransformer.evaluate(record, paras);
    }

    public Transformer getRealTransformer() {
        return realTransformer;
    }
}
