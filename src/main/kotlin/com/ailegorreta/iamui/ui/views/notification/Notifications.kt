/* Copyright (c) 2023, LegoSoft Soluciones, S.C.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are not permitted.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 *  Notifications.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.iamui.ui.views.notification

import com.github.mvysny.karibudsl.v10.*
import com.ailegorreta.iamui.backend.data.dto.Notification
import com.ailegorreta.iamui.backend.data.service.EventService
import com.ailegorreta.iamui.ui.views.ViewFrame
import com.vaadin.flow.component.AttachEvent
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.data.provider.DataProvider
import com.vaadin.flow.data.renderer.ComponentRenderer
import java.time.Duration
import java.time.Instant

/**
 * Page to handle list all persistent notifications.
 *
 * It calls the Audit microservice to get the notification for one-week-old
 *
 *  @author rlh
 *  @project : iam-ui
 *  @date September 2022
 */
class Notifications: ViewFrame {

    private var eventService: EventService

    constructor(eventService: EventService) {
        this.eventService = eventService
        setViewContent(Content(eventService))
    }

    // Main content
    class Content(val service: EventService): KComposite() {
        lateinit var grid: Grid<Notification>

        val root = ui {
                verticalLayout {
                    h2("Notificaciones")
                    grid = grid {
                        columnFor(Notification::title) {
                            isAutoWidth = true
                            setHeader("Tipo")
                        }
                        columnFor(Notification::message) {
                            isAutoWidth = true
                            setHeader("Notificación")
                        }
                        addColumn(ComponentRenderer { notification -> timeRenderer(notification) }).apply {
                            setHeader("")
                        }
                    }
                }
        }

        override fun onAttach(attachEvent: AttachEvent?) {
            super.onAttach(attachEvent)
            grid.setItems(DataProvider.ofCollection(service.notifications()))
        }

        private fun timeRenderer(notification: Notification): Span {
            val time = Duration.between(notification.time, Instant.now())
            val span = Span(prettyMinutes(time.toMinutes()))

            span.className = "text-2xs font-extralight"
            return span
        }

        private fun prettyMinutes(minutes: Long): String {
            if (minutes < 60) {
                if (minutes == 0L) return "En este momento..."
                if (minutes == 1L) return "Hace un minuto..."
                return "Acerca de $minutes minutos"
            }
            if (minutes < 1440) {
                val hours: Long = minutes / 60

                if (hours == 1L) return "Una hora ${(minutes % 60)} minutos"
                return "$hours  horas ${(minutes % 60)} minutos"
            }
            val days:Long = minutes / 1440
            val hours: Long = (minutes - days * 1440) / 60
            val min:Long = minutes - days * 1440  - hours * 60

            if (days == 1L) return "Un día $hours horas $min minutos"
            return "$days días $hours horas $min minutos"
        }
    }
}
