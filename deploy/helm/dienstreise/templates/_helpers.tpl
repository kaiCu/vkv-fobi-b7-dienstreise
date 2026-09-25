{{- define "dienstreise.fullname" -}}
{{- printf "%s" .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "dienstreise.labels" -}}
app.kubernetes.io/name: dienstreise
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{- define "dienstreise.selectorLabels" -}}
app.kubernetes.io/name: dienstreise
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}
