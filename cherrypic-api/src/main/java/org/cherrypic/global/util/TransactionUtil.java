package org.cherrypic.global.util;

import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TransactionUtil {

    @Transactional
    public <T> T getResult(Supplier<T> transactionalTask) {
        return transactionalTask.get();
    }
}
