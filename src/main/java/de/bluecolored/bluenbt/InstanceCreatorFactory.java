package de.bluecolored.bluenbt;

import java.util.Optional;

public interface InstanceCreatorFactory {

    <T> Optional<? extends InstanceCreator<T>> create(TypeToken<T> type, BlueNBT blueNBT);

}
