//
// Created by huangdejun on 2024/6/16.
//

#ifndef OPENGLSAMPLE_UTILS_H
#define OPENGLSAMPLE_UTILS_H

#include <time.h>

long getTimestamp() {
    struct timespec ts{};
    clock_gettime(CLOCK_REALTIME, &ts);
    return ts.tv_sec * 1000 + ts.tv_nsec / 1000000;
}
#endif //OPENGLSAMPLE_UTILS_H
