import React, { useEffect } from 'react'
import { Link } from '../Router'
import { getFirstLine, scrollAndHighlight } from "./helper"
import Layout from "./layout"

const ModuleView = (props) => {
    useEffect(() => {
        if (props.history.location.hash != "") {
            scrollAndHighlight(props.history.location.hash);
        } else {
            window.scrollTo(0, 0);
        }
    });
    return (
        <Layout {...props} title={"API Docs : " + props.module.id} pageType="module">

            <div>

                <h1>{props.module.orgName}/{props.module.id}:{props.module.version}</h1>
                <span dangerouslySetInnerHTML={{ __html: props.module.description }} />

                {props.module.listeners.length > 0 &&
                    <section id="listeners" className="module-construct">
                        <div className="main-method-title here">
                            <h2>Listeners</h2>
                            <p>[{props.module.listeners.length}]</p>
                        </div>
                        <div className="ui divider"></div>
                        <table className="ui very basic table">
                            <tbody>
                                {props.module.listeners.map(item => (
                                    <tr key={item.name}>
                                        <td className="module-title truncate abstractObjects" id={item.name} title={item.name}>
                                            <Link className={item.isDeprecated ? "strike records" : "records"} to={props.module.id + "/listeners/" + item.name}>{item.name}</Link>

                                        </td>
                                        <td className="module-desc">
                                            {
                                                item.isDeprecated == true &&
                                                <div className="ui orange horizontal label" data-tooltip="Deprecated" data-position="top left">D</div>
                                            }
                                            <span dangerouslySetInnerHTML={getFirstLine(item.description)} /></td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </section>
                }

                {props.module.clients.length > 0 &&
                    <section id="clients" className="module-construct">
                        <div className="main-method-title here">
                            <h2>Clients</h2>
                            <p>[{props.module.clients.length}]</p>
                        </div>
                        <div className="ui divider"></div>
                        <table className="ui very basic table">
                            <tbody>
                                {props.module.clients.map(item => (
                                    <tr key={item.name}>
                                        <td className="module-title truncate clients" id={item.name} title={item.name}>
                                            <Link className={item.isDeprecated ? "strike clients" : "clients"} to={props.module.id + "/clients/" + item.name}>{item.name}</Link>

                                        </td>
                                        <td className="module-desc">
                                            {
                                                item.isDeprecated == true &&
                                                <div className="ui orange horizontal label" data-tooltip="Deprecated" data-position="top left">D</div>
                                            }
                                            <span dangerouslySetInnerHTML={getFirstLine(item.description)} /></td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </section>
                }

                {props.module.functions.length > 0 &&
                    <section id="functions" className="module-construct">
                        <div className="main-method-title here">
                            <h2>Functions</h2>
                            <p>[{props.module.functions.length}]</p>
                        </div>
                        <div className="ui divider"></div>
                        <table className="ui very basic table">
                            <tbody>
                                {props.module.functions.map(item => (
                                    <tr key={item.name}>
                                        <td className="module-title truncate functions" id={item.name} title={item.name}>
                                            <Link className={item.isDeprecated ? "strike functions" : "functions"} to={props.module.id + "/functions#" + item.name}>{item.name}</Link>

                                        </td>
                                        <td className="module-desc">
                                            {
                                                item.isDeprecated == true &&
                                                <div className="ui orange horizontal label" data-tooltip="Deprecated" data-position="top left">D</div>
                                            }
                                            <span dangerouslySetInnerHTML={getFirstLine(item.description)} />
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </section>
                }

                {props.module.classes.length > 0 &&
                    <section id="classes" className="module-construct">
                        <div className="main-method-title here">
                            <h2>Classes</h2>
                            <p>[{props.module.classes.length}]</p>
                        </div>
                        <div className="ui divider"></div>
                        <table className="ui very basic table">
                            <tbody>
                                {props.module.classes.map(item => (
                                    <tr key={item.name}>
                                        <td className="module-title truncate classes" id={item.name} title={item.name}>
                                            <Link className={item.isDeprecated ? "strike classes" : "classes"} to={props.module.id + "/classes/" + item.name}>{item.name}</Link>

                                        </td>
                                        <td className="module-desc">
                                            {
                                                item.isDeprecated == true &&
                                                <div className="ui orange horizontal label" data-tooltip="Deprecated" data-position="top left">D</div>
                                            }
                                            <span dangerouslySetInnerHTML={getFirstLine(item.description)} /></td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </section>
                }
                {props.module.abstractObjects.length > 0 &&
                    <section id="abstractObjects" className="module-construct">
                        <div className="main-method-title here">
                            <h2>Abstract Objects </h2>
                            <p>[{props.module.abstractObjects.length}]</p>
                        </div>
                        <div className="ui divider"></div>
                        <table className="ui very basic table">
                            <tbody>
                                {props.module.abstractObjects.map(item => (
                                    <tr key={item.name}>
                                        <td className="module-title truncate abstractObjects" id={item.name} title={item.name}>
                                            <Link className={item.isDeprecated ? "strike abstractObjects" : "abstractObjects"} to={props.module.id + "/abstractObjects/" + item.name}>{item.name}</Link>

                                        </td>
                                        <td className="module-desc">
                                            {
                                                item.isDeprecated == true &&
                                                <div className="ui orange horizontal label" data-tooltip="Deprecated" data-position="top left">D</div>
                                            }
                                            <span dangerouslySetInnerHTML={getFirstLine(item.description)} /></td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </section>
                }

                {props.module.records.length > 0 &&
                    <section id="records" className="module-construct">
                        <div className="main-method-title here">
                            <h2>Records </h2>
                            <p>[{props.module.records.length}]</p>
                        </div>
                        <div className="ui divider"></div>
                        <table className="ui very basic table">
                            <tbody>
                                {props.module.records.map(item => (
                                    <tr key={item.name}>
                                        <td className="module-title truncate records" id={item.name} title={item.name}>
                                            <Link className={item.isDeprecated ? "strike records" : "records"} to={props.module.id + "/records/" + item.name}>{item.name}</Link>

                                        </td>
                                        <td className="module-desc">
                                            {
                                                item.isDeprecated == true &&
                                                <div className="ui orange horizontal label" data-tooltip="Deprecated" data-position="top left">D</div>
                                            }
                                            <span dangerouslySetInnerHTML={getFirstLine(item.description)} /></td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </section>
                }

                {props.module.constants.length > 0 &&
                    <section id="constants" className="module-construct">
                        <div className="main-method-title here">
                            <h2>Constants</h2>
                            <p>[{props.module.constants.length}]</p>
                        </div>
                        <div className="ui divider"></div>
                        <table className="ui very basic table">
                            <tbody>
                                {props.module.constants.map(item => (
                                    <tr key={item.name}>
                                        <td className="module-title truncate constants" id={item.name} title={item.name}>
                                            <Link className={item.isDeprecated ? "strike constants" : "constants"} to={props.module.id + "/constants#" + item.name}>{item.name}</Link>

                                        </td>
                                        <td className="module-desc">
                                            {
                                                item.isDeprecated == true &&
                                                <div className="ui orange horizontal label" data-tooltip="Deprecated" data-position="top left">D</div>
                                            }
                                            <span dangerouslySetInnerHTML={getFirstLine(item.description)} /></td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </section>
                }


                {props.module.annotations.length > 0 &&
                    <section id="annotations" className="module-construct">
                        <div className="main-method-title here">
                            <h2>Annotations</h2>
                            <p>[{props.module.annotations.length}]</p>
                        </div>
                        <div className="ui divider"></div>
                        <table className="ui very basic table">
                            <tbody>
                                {props.module.annotations.map(item => (
                                    <tr key={item.name}>
                                        <td className="module-title truncate annotations" id={item.name} title={item.name}>
                                            <Link className={item.isDeprecated ? "strike annotations" : "annotations"} to={props.module.id + "/annotations#" + item.name}>{item.name}</Link>

                                        </td>
                                        <td className="module-desc">
                                            {
                                                item.isDeprecated == true &&
                                                <div className="ui orange horizontal label" data-tooltip="Deprecated" data-position="top left">D</div>
                                            }
                                            <span dangerouslySetInnerHTML={getFirstLine(item.description)} /></td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </section>
                }

                {props.module.types.length > 0 &&
                    <section id="types" className="module-construct">
                        <div className="main-method-title here">
                            <h2>Types</h2>
                            <p>[{props.module.types.length}]</p>
                        </div>
                        <div className="ui divider"></div>
                        <table className="ui very basic table">
                            <tbody>
                                {props.module.types.map(item => (
                                    <tr key={item.name}>
                                        <td className="module-title truncate types" id={item.name} title={item.name}>
                                            <Link className={item.isDeprecated ? "strike types" : "types"} to={props.module.id + "/types#" + item.name}>{item.name}</Link>

                                        </td>
                                        <td className="module-desc">
                                            {
                                                item.isDeprecated == true &&
                                                <div className="ui orange horizontal label" data-tooltip="Deprecated" data-position="top left">D</div>
                                            }
                                            <span dangerouslySetInnerHTML={getFirstLine(item.description)} /></td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </section>
                }

                {props.module.errors.length > 0 &&
                    <section id="errors" className="module-construct">
                        <div className="main-method-title here">
                            <h2>Errors</h2>
                            <p>[{props.module.errors.length}]</p>
                        </div>
                        <div className="ui divider"></div>
                        <table className="ui very basic table">
                            <tbody>
                                {props.module.errors.map(item => (
                                    <tr key={item.name}>
                                        <td className="module-title truncate errors" id={item.name} title={item.name}>
                                            <Link className={item.isDeprecated ? "strike errors" : "errors"} to={props.module.id + "/errors#" + item.name}>{item.name}</Link>

                                        </td>
                                        <td className="module-desc">
                                            {
                                                item.isDeprecated == true &&
                                                <div className="ui orange horizontal label" data-tooltip="Deprecated" data-position="top left">D</div>
                                            }
                                            <span dangerouslySetInnerHTML={getFirstLine(item.description)} /></td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </section>
                }
            </div>
        </Layout>
    );
}

export default ModuleView;