/*
* Copyright @ 2018 - present 8x8, Inc.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.jitsi.videobridge.ice

import org.ice4j.ice.harvest.SinglePortUdpHarvester
import org.ice4j.ice.harvest.TcpHarvester
import org.jitsi.utils.logging2.createLogger
import java.io.IOException

class Harvesters private constructor(
    val tcpHarvester: TcpHarvester?,
    val singlePortHarvesters: List<SinglePortUdpHarvester>
) {
    /* We're unhealthy if there are no single port harvesters. */
    val healthy: Boolean
        get() = singlePortHarvesters.isNotEmpty()

    private fun close() {
        singlePortHarvesters.forEach { it.close() }
        tcpHarvester?.close()
    }

    companion object {
        private val logger = createLogger()

        fun init() {
            // Trigger the lazy init.
            INSTANCE
        }

        fun close() = INSTANCE.close()

        val INSTANCE: Harvesters by lazy {
            val singlePortHarvesters = SinglePortUdpHarvester.createHarvesters(IceConfig.config.port)
            if (singlePortHarvesters.isEmpty()) {
                logger.warn("No single-port harvesters created.")
            }
            val tcpHarvester: TcpHarvester? = if (IceConfig.config.tcpEnabled) {
                val port = IceConfig.config.tcpPort
                try {
                    TcpHarvester(IceConfig.config.port, IceConfig.config.iceSslTcp).apply {
                        logger.info("Initialized TCP harvester on port $port, ssltcp=${IceConfig.config.iceSslTcp}")
                        IceConfig.config.tcpMappedPort?.let { mappedPort ->
                            logger.info("Adding mapped port $mappedPort")
                            addMappedPort(mappedPort)
                        }
                    }
                } catch (ioe: IOException) {
                    logger.warn("Failed to initialize TCP harvester on port $port")
                    null
                }
            } else {
                null
            }

            Harvesters(tcpHarvester, singlePortHarvesters)
        }
    }
}
