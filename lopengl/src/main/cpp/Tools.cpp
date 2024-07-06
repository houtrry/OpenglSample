//
// Created by houtr on 2024-06-18.
//

#include "Tools.h"
#include <time.h>

long Tools::getTimestamp() {
    struct timespec ts{};
    clock_gettime(CLOCK_REALTIME, &ts);
    return ts.tv_sec * 1000 + ts.tv_nsec / 1000000;
}